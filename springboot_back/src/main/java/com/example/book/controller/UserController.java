package com.example.book.controller;

import com.example.book.annotation.RequireRole;
import com.example.book.common.BusinessException;
import com.example.book.common.Result;
import com.example.book.dto.ChangePasswordDTO;
import com.example.book.dto.UpdateProfileDTO;
import com.example.book.entity.User;
import com.example.book.service.IUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 读者个人中心接口（登录用户均可访问：普通读者、图书管理员）
 * 个人信息查询/维护、头像上传、修改密码
 */
@RestController
@RequestMapping("/api/user")
@RequireRole({"ADMIN", "USER"})
public class UserController {

    /** 允许的头像图片扩展名 */
    private static final Set<String> ALLOWED_AVATAR_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    /** 头像大小上限：2MB */
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;

    /** 头像保存目录（与 application.yml 的 app.upload-dir 保持一致） */
    @Value("${app.upload-dir:./uploads/avatars}")
    private String uploadDir;

    private final IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    /**
     * 查询当前登录用户的个人信息
     * GET /api/user/profile
     */
    @GetMapping("/profile")
    public Result<User> profile(HttpServletRequest request) {
        return Result.success(userService.getProfile(currentUserId(request)));
    }

    /**
     * 修改当前登录用户的个人信息（昵称/邮箱/手机号）
     * PUT /api/user/profile
     * 请求体：{ nickname, email, phone }
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(HttpServletRequest request, @RequestBody @Valid UpdateProfileDTO dto) {
        userService.updateProfile(currentUserId(request), dto);
        return Result.success("个人信息修改成功");
    }

    /**
     * 修改密码（需校验原密码）
     * PUT /api/user/password
     * 请求体：{ oldPassword, newPassword }
     */
    @PutMapping("/password")
    public Result<Void> changePassword(HttpServletRequest request, @RequestBody @Valid ChangePasswordDTO dto) {
        userService.changePassword(currentUserId(request), dto);
        return Result.success("密码修改成功");
    }

    /**
     * 上传头像（multipart/form-data，字段名 file）
     * POST /api/user/avatar
     * 返回：{ url: "/uploads/avatars/xxx.png" }
     */
    @PostMapping("/avatar")
    public Result<Map<String, Object>> uploadAvatar(HttpServletRequest request,
                                                    @RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException(400, "图片大小不能超过2MB");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(original);
        if (ext == null || !ALLOWED_AVATAR_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new BusinessException(400, "仅支持 jpg/png/gif/webp 格式的图片");
        }

        Integer userId = currentUserId(request);
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        // 文件名带用户ID与时间戳，避免重名与浏览器缓存旧图
        String filename = "avatar_" + userId + "_" + System.currentTimeMillis() + "." + ext.toLowerCase();
        Path target = dir.resolve(filename);
        file.transferTo(target);

        // 删除旧头像文件（仅限上传目录内的历史头像，防止误删其他文件）
        String oldAvatar = userService.getProfile(userId).getAvatar();
        if (StringUtils.hasText(oldAvatar) && oldAvatar.startsWith("/uploads/avatars/")) {
            Path oldFile = dir.resolve(oldAvatar.substring("/uploads/avatars/".length())).normalize();
            if (oldFile.startsWith(dir)) {
                Files.deleteIfExists(oldFile);
            }
        }

        String url = "/uploads/avatars/" + filename;
        userService.updateAvatar(userId, url);

        Map<String, Object> data = new HashMap<>();
        data.put("url", url);
        return Result.success("头像上传成功", data);
    }

    /** 从拦截器写入的请求属性中取当前登录用户ID */
    private Integer currentUserId(HttpServletRequest request) {
        return (Integer) request.getAttribute("userId");
    }
}
