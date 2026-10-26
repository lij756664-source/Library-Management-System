package com.example.book.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardVO {

    private Long totalBooks;
    private Long totalStock;
    private Long borrowingCount;
    private Long overdueCount;
    private BigDecimal totalFine;
    private Long userCount;
}
