package com.example.book.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Category {

    private Integer id;
    private String name;
    private String description;
    private LocalDateTime createTime;
}
