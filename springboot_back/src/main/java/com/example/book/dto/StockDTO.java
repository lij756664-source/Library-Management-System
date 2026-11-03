package com.example.book.dto;

import lombok.Data;

/** 馆藏副本调整请求，数量范围由图书服务统一校验。 */
@Data
public class StockDTO {
    private Integer delta;
}
