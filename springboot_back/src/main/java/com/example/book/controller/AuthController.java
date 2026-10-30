package com.example.book.controller;

import com.example.book.common.Result;
import com.example.book.dto.LoginDTO;
import com.example.book.dto.RegisterDTO;
import com.example.book.entity.User;
import com.example.book.service.IUserService;
import com.example.book.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 身份认证接口（公开，无需登录）
 * 注册、登录
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IUserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(IUserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 用户注册（公开接口）
     * POST /api/auth/register
     * 请求体：{ username, password, nickname(选填) }
     * 注册成功后角色默认为普通读者 USER
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid RegisterDTO dto) {
        userService.register(dto);
        return Result.success("注册成功");
    }

    /**
     * 账号登录（公开接口）
     * POST /api/auth/login
     * 请求体：{ username, password }
     * 登录成功后返回用户信息（不含密码）与 JWT 令牌，
     * 后续请求需在请求头携带 Authorization: Bearer <token>
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @Valid LoginDTO dto) {
        User user = userService.login(dto.getUsername(), dto.getPassword());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("token", token);
        return Result.success("登录成功", data);
    }
}
