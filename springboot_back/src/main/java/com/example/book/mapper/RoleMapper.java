package com.example.book.mapper;

import com.example.book.entity.Role;
import org.apache.ibatis.annotations.Param;

public interface RoleMapper {

    /** 按角色编码查询角色 */
    Role findByCode(@Param("code") String code);
}
