package com.example.book.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志表实体
 * 记录所有账号相关操作（注册/登录/修改个人信息/修改密码/启用停用账号等），便于后续追溯
 */
@Data
public class OperationLog {

    private Integer id;
    /** 操作人用户ID（登录失败等未登录场景为 null） */
    private Integer userId;
    /** 操作人用户名 */
    private String username;
    /** 操作类型：注册/登录/修改个人信息/修改密码/启用账号/停用账号/重置密码 等 */
    private String operation;
    /** 操作详情 */
    private String detail;
    /** 操作来源IP */
    private String ip;
    private LocalDateTime createTime;
}
