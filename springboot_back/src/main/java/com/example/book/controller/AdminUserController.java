package com.example.book.controller;

import com.example.book.annotation.RequireRole;
import com.example.book.common.PageResult;
import com.example.book.common.Result;
import com.example.book.dto.ResetPasswordDTO;
import com.example.book.dto.StatusDTO;
import com.example.book.dto.UpdateProfileDTO;
import com.example.book.entity.User;
import com.example.book.service.IUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理员-读者账号管理接口（仅图书管理员可访问，普通读者访问返回 403）
 * 账号列表查询、账号详情、启用/停用、账号信息管理、重置密码
 */
@RestController
@RequestMapping("/api/admin/users")
@RequireRole("ADMIN")
public class AdminUserController {

    private final IUserService userService;

    public AdminUserController(IUserService userService) {
        this.userService = userService;
    }

    /**
     * 分页查询账号列表
     * GET /api/admin/users?page=1&size=10&username=&status=&role=
     * username：用户名/昵称模糊筛选（可选）
     * status：1-启用 0-停用（可选）
     * role：ADMIN/USER（可选）
     */
    @GetMapping
    public Result<PageResult<User>> pageUsers(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String username,
                                              @RequestParam(required = false) Integer status,
                                              @RequestParam(required = false) String role) {
        return Result.success(userService.pageUsers(page, size, username, status, role));
    }

    /**
     * 查询账号详情（含角色列表）
     * GET /api/admin/users/{id}
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Integer id) {
        Map<String, Object> data = new HashMap<>();
        data.put("user", userService.getUserDetail(id));
        data.put("roles", userService.getUserRoles(id));
        return Result.success(data);
    }

    /**
     * 账号启用/停用（仅限读者账号）
     * PUT /api/admin/users/{id}/status
     * 请求体：{ status: 1-启用 0-停用 }
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Integer id, @RequestBody @Valid StatusDTO dto,
                                     HttpServletRequest request) {
        userService.updateStatus(currentUserId(request), currentUsername(request), id, dto.getStatus());
        return Result.success(dto.getStatus() == 1 ? "账号已启用" : "账号已停用");
    }

    /**
     * 修改账号信息（仅限读者账号）
     * PUT /api/admin/users/{id}
     * 请求体：{ nickname, email, phone }
     */
    @PutMapping("/{id}")
    public Result<Void> updateInfo(@PathVariable Integer id, @RequestBody @Valid UpdateProfileDTO dto,
                                   HttpServletRequest request) {
        userService.updateUserInfo(currentUserId(request), currentUsername(request), id, dto);
        return Result.success("账号信息修改成功");
    }

    /**
     * 重置账号密码（仅限读者账号）
     * PUT /api/admin/users/{id}/password
     * 请求体：{ password }
     */
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Integer id, @RequestBody @Valid ResetPasswordDTO dto,
                                      HttpServletRequest request) {
        userService.resetPassword(currentUserId(request), currentUsername(request), id, dto.getPassword());
        return Result.success("密码重置成功");
    }

    /** 从拦截器写入的请求属性中取当前登录用户ID */
    private Integer currentUserId(HttpServletRequest request) {
        return (Integer) request.getAttribute("userId");
    }

    /** 从拦截器写入的请求属性中取当前登录用户名 */
    private String currentUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }
}
