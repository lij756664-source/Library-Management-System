package com.example.book.mapper;

import com.example.book.entity.Role;
import com.example.book.entity.UserRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserRoleMapper {

    /** 新增用户-角色关联 */
    int insert(UserRole userRole);

    /** 查询用户拥有的角色列表 */
    List<Role> findRolesByUserId(@Param("userId") Integer userId);
}
