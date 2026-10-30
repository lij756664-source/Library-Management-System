package com.example.book.controller;

import com.example.book.annotation.RequireRole;
import com.example.book.common.PageResult;
import com.example.book.common.Result;
import com.example.book.entity.OperationLog;
import com.example.book.service.IOperationLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员-操作日志查询接口（仅图书管理员可访问，便于追溯账号相关操作）
 */
@RestController
@RequestMapping("/api/admin/logs")
@RequireRole("ADMIN")
public class AdminLogController {

    private final IOperationLogService operationLogService;

    public AdminLogController(IOperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    /**
     * 分页查询操作日志
     * GET /api/admin/logs?page=1&size=10&username=&operation=
     * username：操作人用户名模糊筛选（可选）
     * operation：操作类型精确筛选，如 注册/登录/修改密码/停用账号（可选）
     */
    @GetMapping
    public Result<PageResult<OperationLog>> pageLogs(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(required = false) String username,
                                                     @RequestParam(required = false) String operation) {
        return Result.success(operationLogService.pageLogs(page, size, username, operation));
    }
}
