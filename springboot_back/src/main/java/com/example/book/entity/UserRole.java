package com.example.book.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户-角色关联表实体
 * 注册时与 user.role 冗余字段同步写入，用于规范化的角色关联查询
 */
@Data
public class UserRole {

    private Integer id;
    /** 用户ID */
    private Integer userId;
    /** 角色ID */
    private Integer roleId;
    private LocalDateTime createTime;
}
