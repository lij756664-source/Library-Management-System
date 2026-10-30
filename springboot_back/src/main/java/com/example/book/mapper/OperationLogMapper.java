package com.example.book.mapper;

import com.example.book.entity.OperationLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OperationLogMapper {

    /** 新增操作日志 */
    int insert(OperationLog log);

    /** 分页查询操作日志（按用户名模糊、操作类型精确筛选） */
    List<OperationLog> selectPage(@Param("offset") Integer offset, @Param("size") Integer size,
                                  @Param("username") String username, @Param("operation") String operation);

    /** 分页查询总数 */
    int countPage(@Param("username") String username, @Param("operation") String operation);
}
