package com.example.book.service.impl;

import com.example.book.common.PageResult;
import com.example.book.entity.OperationLog;
import com.example.book.mapper.OperationLogMapper;
import com.example.book.service.IOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(OperationLogServiceImpl.class);
    /** 日志字段长度上限（与数据库字段保持一致），超长内容截断，避免审计写入失败 */
    private static final int MAX_OPERATION_LENGTH = 50;
    private static final int MAX_DETAIL_LENGTH = 500;

    private final OperationLogMapper operationLogMapper;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    /**
     * 记录操作日志
     * <p>审计日志属于旁路能力：写入失败（如日志表异常）时只记录服务端错误日志，
     * 不向调用方抛出异常，避免因为写日志失败而让已完成的主业务操作返回失败给用户。</p>
     */
    @Override
    public void record(Integer userId, String username, String operation, String detail) {
        try {
            OperationLog entity = new OperationLog();
            entity.setUserId(userId);
            entity.setUsername(username);
            entity.setOperation(truncate(operation, MAX_OPERATION_LENGTH));
            entity.setDetail(truncate(detail, MAX_DETAIL_LENGTH));
            entity.setIp(getClientIp());
            operationLogMapper.insert(entity);
        } catch (Exception e) {
            log.error("操作日志写入失败，operation={}，username={}，detail={}", operation, username, detail, e);
        }
    }

    /** 超长内容截断，保证日志写入不会因字段长度溢出失败 */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
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
