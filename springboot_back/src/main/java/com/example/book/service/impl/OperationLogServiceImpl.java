package com.example.book.service.impl;

import com.example.book.common.PageResult;
import com.example.book.entity.OperationLog;
import com.example.book.mapper.OperationLogMapper;
import com.example.book.service.IOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 操作日志服务实现
 * 所有账号相关操作（注册/登录/修改资料/修改密码/启用停用等）均写入操作日志，便于追溯
 */
@Service
public class OperationLogServiceImpl implements IOperationLogService {

    private final OperationLogMapper operationLogMapper;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    public void record(Integer userId, String username, String operation, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setOperation(operation);
        log.setDetail(detail);
        log.setIp(getClientIp());
        operationLogMapper.insert(log);
    }

    @Override
    public PageResult<OperationLog> pageLogs(int page, int size, String username, String operation) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        long total = operationLogMapper.countPage(username, operation);
        List<OperationLog> list = operationLogMapper.selectPage((p - 1) * s, s, username, operation);
        return PageResult.of(total, list);
    }

    /** 从当前请求上下文获取客户端IP（支持反向代理场景） */
    private String getClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含多级代理地址，仅取第一个，并限制长度
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        if (ip != null && ip.length() > 50) {
            ip = ip.substring(0, 50);
        }
        return ip;
    }
}
