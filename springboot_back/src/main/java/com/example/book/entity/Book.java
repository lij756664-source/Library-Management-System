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
}
