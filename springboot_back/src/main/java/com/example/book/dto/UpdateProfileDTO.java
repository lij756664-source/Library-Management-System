package com.example.book.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人信息请求参数（读者本人修改、管理员修改账号信息共用）
 */
@Data
public class UpdateProfileDTO {

    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 20, message = "手机号长度不能超过20个字符")
    private String phone;
}
