package com.example.book.vo;

import lombok.Data;

@Data
public class BookStatVO {

    private Integer bookId;
    private String bookTitle;
    private String categoryName;
    private Long borrowCount;
}
