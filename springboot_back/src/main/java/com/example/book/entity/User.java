package com.example.book.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {

    private Integer id;
    private String username;
    /** 密码（BCrypt 加密存储，禁止明文；对外返回前必须置 null） */
    private String password;
    private String nickname;
    /** 邮箱（选填） */
    private String email;
    /** 手机号（选填） */
    private String phone;
    /** 头像地址（上传后为 /uploads/avatars/xxx 形式，选填） */
    private String avatar;
    /** 角色编码：ADMIN-图书管理员 USER-普通读者（与 user_role 关联表同步维护） */
    private String role;
    /** 账号状态：1-启用 0-停用 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
