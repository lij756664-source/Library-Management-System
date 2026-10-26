package com.example.book.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BorrowRecord {

    private Integer id;
    private Integer userId;
    private Integer bookId;
    private LocalDateTime borrowTime;
    private LocalDateTime dueTime;
    private LocalDateTime returnTime;
    private String status;
    private Integer renewCount;
    private Integer overdueDays;
    private BigDecimal fineAmount;

    /** 关联展示字段 */
    private String username;
    private String nickname;
    private String bookTitle;
    private String bookAuthor;
    private String categoryName;
    /** 计算字段：是否逾期 */
    private Boolean overdue;
    /** 剩余天数（负数表示已逾期天数） */
    private Long remainDays;
}
