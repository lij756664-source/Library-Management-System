package com.example.book.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色表实体
 * code：ADMIN-图书管理员，USER-普通读者
 */
@Data
public class Role {

    private Integer id;
    /** 角色编码 */
    private String code;
    /** 角色名称 */
    private String name;
    /** 角色描述 */
    private String description;
    private LocalDateTime createTime;
}
