package com.example.book.service;

import java.time.LocalDate;
import java.util.Map;

/** 统计页统一入口，日期仅筛选分类与排行榜。 */
public interface IStatisticsService {
    Map<String, Object> query(LocalDate start, LocalDate end);
}
