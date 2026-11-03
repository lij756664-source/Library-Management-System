package com.example.book.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

/** 按北京时间自然日计算罚金，不读写数据库。 */
public interface IFineService {
    ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    record Result(int overdueDays, BigDecimal amount) {}

    /** 到期当天不计逾期；daily 使用借阅时保存的日费。 */
    Result calculate(LocalDate due, BigDecimal daily, LocalDate date);
}
