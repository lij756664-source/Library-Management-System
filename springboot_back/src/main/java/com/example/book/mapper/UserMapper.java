package com.example.book.mapper;

import com.example.book.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper {

    /** 按用户名查询（含密码，仅内部校验使用） */
    User findByUsername(@Param("username") String username);

    /** 按ID查询（含密码，仅内部校验使用） */
    User findById(@Param("id") Integer id);

    /** 新增用户 */
    int insert(User user);

    /** 修改个人信息（昵称/邮箱/手机号） */
    int updateProfile(@Param("id") Integer id, @Param("nickname") String nickname,
                      @Param("email") String email, @Param("phone") String phone);

    /** 修改密码 */
    int updatePassword(@Param("id") Integer id, @Param("password") String password);

    /** 更新头像地址 */
    int updateAvatar(@Param("id") Integer id, @Param("avatar") String avatar);

    /** 启用/停用账号 */
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    /** 分页查询账号列表（不查密码，支持用户名/昵称模糊、状态、角色筛选） */
    List<User> selectPage(@Param("offset") Integer offset, @Param("size") Integer size,
                          @Param("username") String username, @Param("status") Integer status,
                          @Param("role") String role);

    /** 分页查询总数 */
    int countPage(@Param("username") String username, @Param("status") Integer status,
                  @Param("role") String role);
}
