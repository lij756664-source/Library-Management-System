package com.example.book.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 馆藏副本（库存）增减请求参数
 */
@Data
public class StockDTO {

    /** 增量：正数增加副本，负数减少副本 */
    @NotNull(message = "库存调整量不能为空")
    private Integer delta;
}
