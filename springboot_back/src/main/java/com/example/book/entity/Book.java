package com.example.book.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Book {

    private Integer id;
    private String title;
    private String author;
    private String isbn;
    private Integer categoryId;
    private String categoryName;
    private BigDecimal price;
    private Integer stock;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 当前排队预约人数（WAITING + READY） */
    private Integer waitingCount;
    /** 当前用户对该书的预约状态 */
    private String myReserveStatus;
}
