package com.example.book.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Reservation {

    private Integer id;
    private Integer userId;
    private Integer bookId;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime expireTime;
    private LocalDateTime updateTime;

    /** 关联展示字段 */
    private String username;
    private String nickname;
    private String bookTitle;
    private String bookAuthor;
    private String categoryName;
    /** 当前排队序号（待取书为 1） */
    private Integer queueNo;
    /** 同书排队总人数（WAITING + READY） */
    private Integer queueSize;
}
