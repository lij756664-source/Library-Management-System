package com.example.book.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 账号启用/停用请求参数
 */
@Data
public class StatusDTO {

    /** 账号状态：1-启用 0-停用 */
    @NotNull(message = "账号状态不能为空")
    private Integer status;
}
