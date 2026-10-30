package com.example.book.service;

import com.example.book.common.PageResult;
import com.example.book.entity.OperationLog;

public interface IOperationLogService {

    /**
     * 记录一条操作日志
     *
     * @param userId    操作人用户ID（登录失败等未登录场景可传 null）
     * @param username  操作人用户名
     * @param operation 操作类型：注册/登录/修改个人信息/修改密码/启用账号/停用账号/重置密码 等
     * @param detail    操作详情
     */
    void record(Integer userId, String username, String operation, String detail);

    /**
     * 【管理员】分页查询操作日志
     *
     * @param page      页码（从1开始）
     * @param size      每页条数
     * @param username  用户名模糊筛选（可选）
     * @param operation 操作类型精确筛选（可选）
     */
    PageResult<OperationLog> pageLogs(int page, int size, String username, String operation);
}
