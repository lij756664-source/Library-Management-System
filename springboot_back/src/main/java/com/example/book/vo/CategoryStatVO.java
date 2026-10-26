package com.example.book.vo;

import lombok.Data;

@Data
public class CategoryStatVO {

    private String categoryName;
    private Long borrowCount;
    private Long overdueCount;
}
