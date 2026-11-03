package com.example.book.service;

import com.example.book.entity.BorrowRecord;

import java.util.List;
import java.util.Map;

public interface IBorrowService {

    /** 图书借阅 */
    void borrowBook(Integer userId, Integer bookId);

    /** 图书续借 */
    void renewBook(Integer userId, Integer borrowId);

    /** 图书归还；isAdmin 为 true 时允许代还 */
    void returnBook(Integer userId, Integer borrowId, boolean isAdmin);

    /** 读者本人借阅记录 */
    List<BorrowRecord> myBorrows(Integer userId, String status, String keyword);

    /** 管理员全馆借阅记录 */
    List<BorrowRecord> allBorrows(String keyword, String status);

    /** 借期、续借、罚款等规则 */
    Map<String, String> getBorrowRules();
}
