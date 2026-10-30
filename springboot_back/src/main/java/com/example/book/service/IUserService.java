package com.example.book.service;

import com.example.book.common.PageResult;
import com.example.book.dto.ChangePasswordDTO;
import com.example.book.dto.RegisterDTO;
import com.example.book.dto.UpdateProfileDTO;
import com.example.book.entity.Role;
import com.example.book.entity.User;

import java.util.List;

/**
 * 用户与权限服务
 * 读者功能：注册、登录、个人信息维护、修改密码
 * 管理员功能：账号列表查询、账号详情、启用/停用、账号信息管理、重置密码
 */
public interface IUserService {

    /** 用户注册（默认普通读者角色） */
    void register(RegisterDTO dto);

    /** 登录校验：账号存在 + 密码匹配（BCrypt） + 账号处于启用状态，成功后返回不含密码的用户信息 */
    User login(String username, String password);

    /** 查询当前登录用户个人信息（不含密码） */
    User getProfile(Integer userId);

    /** 修改当前登录用户个人信息（昵称/邮箱/手机号） */
    void updateProfile(Integer userId, UpdateProfileDTO dto);

    /** 修改密码（需校验原密码） */
    void changePassword(Integer userId, ChangePasswordDTO dto);

    /** 更新头像（保存 /uploads/avatars/ 下的相对地址） */
    void updateAvatar(Integer userId, String avatarUrl);

    /** 【管理员】分页查询账号列表 */
    PageResult<User> pageUsers(int page, int size, String username, Integer status, String role);

    /** 【管理员】查询账号详情（不含密码） */
    User getUserDetail(Integer id);

    /** 【管理员】查询账号拥有的角色列表 */
    List<Role> getUserRoles(Integer userId);

    /** 【管理员】启用/停用读者账号（1-启用 0-停用，仅限读者账号） */
    void updateStatus(Integer operatorId, String operatorName, Integer id, Integer status);

    /** 【管理员】修改读者账号信息（仅限读者账号） */
    void updateUserInfo(Integer operatorId, String operatorName, Integer id, UpdateProfileDTO dto);

    /** 【管理员】重置读者账号密码（仅限读者账号） */
    void resetPassword(Integer operatorId, String operatorName, Integer id, String password);
}
