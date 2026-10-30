package com.example.book.service.impl;

import com.example.book.common.BusinessException;
import com.example.book.common.PageResult;
import com.example.book.dto.ChangePasswordDTO;
import com.example.book.dto.RegisterDTO;
import com.example.book.dto.UpdateProfileDTO;
import com.example.book.entity.Role;
import com.example.book.entity.User;
import com.example.book.entity.UserRole;
import com.example.book.mapper.RoleMapper;
import com.example.book.mapper.UserMapper;
import com.example.book.mapper.UserRoleMapper;
import com.example.book.service.IOperationLogService;
import com.example.book.service.IUserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户与权限服务实现
 * 读者功能：注册、登录、个人信息维护、修改密码
 * 管理员功能：账号列表查询、启用/停用、账号信息管理、重置密码
 */
@Service
public class UserServiceImpl implements IUserService {

    /** BCrypt 密码加密器（线程安全，全局复用；禁止明文保存密码） */
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final IOperationLogService operationLogService;

    public UserServiceImpl(UserMapper userMapper, RoleMapper roleMapper,
                           UserRoleMapper userRoleMapper, IOperationLogService operationLogService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.operationLogService = operationLogService;
    }

    @Override
    public void register(RegisterDTO dto) {
        User exist = userMapper.findByUsername(dto.getUsername());
        if (exist != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(PASSWORD_ENCODER.encode(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);

        // 同步写入 user_role 关联表（与 user.role 冗余字段保持一致）
        Role readerRole = roleMapper.findByCode("USER");
        if (readerRole != null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(readerRole.getId());
            userRoleMapper.insert(userRole);
        }

        operationLogService.record(user.getId(), user.getUsername(), "注册", "新用户注册成功");
    }

    @Override
    public User login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null || !matches(password, user.getPassword())) {
            operationLogService.record(null, username, "登录", "登录失败：用户名或密码错误");
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            operationLogService.record(user.getId(), username, "登录", "登录失败：账号已被停用");
            throw new BusinessException(403, "账号已被停用，请联系管理员");
        }

        operationLogService.record(user.getId(), username, "登录", "登录成功");
        user.setPassword(null);
        return user;
    }

    @Override
    public User getProfile(Integer userId) {
        User user = requireUser(userId);
        user.setPassword(null);
        return user;
    }

    @Override
    public void updateProfile(Integer userId, UpdateProfileDTO dto) {
        User user = requireUser(userId);
        userMapper.updateProfile(userId, dto.getNickname(), dto.getEmail(), dto.getPhone());
        operationLogService.record(userId, user.getUsername(), "修改个人信息", "用户修改昵称/邮箱/手机号");
    }

    @Override
    public void changePassword(Integer userId, ChangePasswordDTO dto) {
        User user = requireUser(userId);
        if (!matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }
        if (dto.getNewPassword().equals(dto.getOldPassword())) {
            throw new BusinessException(400, "新密码不能与原密码相同");
        }
        userMapper.updatePassword(userId, PASSWORD_ENCODER.encode(dto.getNewPassword()));
        operationLogService.record(userId, user.getUsername(), "修改密码", "用户修改登录密码");
    }

    @Override
    public void updateAvatar(Integer userId, String avatarUrl) {
        User user = requireUser(userId);
        userMapper.updateAvatar(userId, avatarUrl);
        operationLogService.record(userId, user.getUsername(), "更新头像", "用户更新了个人头像");
    }

    @Override
    public PageResult<User> pageUsers(int page, int size, String username, Integer status, String role) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        long total = userMapper.countPage(username, status, role);
        List<User> list = userMapper.selectPage((p - 1) * s, s, username, status, role);
        return PageResult.of(total, list);
    }

    @Override
    public User getUserDetail(Integer id) {
        User user = requireUser(id);
        user.setPassword(null);
        return user;
    }

    @Override
    public List<Role> getUserRoles(Integer userId) {
        return userRoleMapper.findRolesByUserId(userId);
    }

    @Override
    public void updateStatus(Integer operatorId, String operatorName, Integer id, Integer status) {
        User target = requireUser(id);
        if ("ADMIN".equals(target.getRole())) {
            throw new BusinessException(400, "不能对管理员账号执行启用/停用操作");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "账号状态值非法，仅支持 1-启用 0-停用");
        }
        userMapper.updateStatus(id, status);
        operationLogService.record(operatorId, operatorName, status == 1 ? "启用账号" : "停用账号",
                (status == 1 ? "启用" : "停用") + "读者账号【" + target.getUsername() + "】");
    }

    @Override
    public void updateUserInfo(Integer operatorId, String operatorName, Integer id, UpdateProfileDTO dto) {
        User target = requireUser(id);
        if ("ADMIN".equals(target.getRole())) {
            throw new BusinessException(400, "只能管理读者账号");
        }
        userMapper.updateProfile(id, dto.getNickname(), dto.getEmail(), dto.getPhone());
        operationLogService.record(operatorId, operatorName, "修改账号信息",
                "管理员修改读者账号【" + target.getUsername() + "】的资料");
    }

    @Override
    public void resetPassword(Integer operatorId, String operatorName, Integer id, String password) {
        User target = requireUser(id);
        if ("ADMIN".equals(target.getRole())) {
            throw new BusinessException(400, "只能重置读者账号密码");
        }
        userMapper.updatePassword(id, PASSWORD_ENCODER.encode(password));
        operationLogService.record(operatorId, operatorName, "重置密码",
                "管理员重置读者账号【" + target.getUsername() + "】的密码");
    }

    /**
     * BCrypt 密码比对
     * 历史库中若残留未升级的明文密码，BCrypt 解析会抛异常，直接判定不匹配
     */
    private boolean matches(String rawPassword, String encodedPassword) {
        try {
            return PASSWORD_ENCODER.matches(rawPassword, encodedPassword);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** 查询用户，不存在则抛业务异常 */
    private User requireUser(Integer id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }
}
