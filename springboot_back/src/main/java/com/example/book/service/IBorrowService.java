package com.example.book.service;

import com.example.book.entity.BorrowRecord;

import java.util.List;
import java.util.Map;

public interface IBorrowService {

    void borrowBook(Integer userId, Integer bookId);

    void renewBook(Integer userId, Integer borrowId);

    void returnBook(Integer userId, Integer borrowId, boolean isAdmin);

    List<BorrowRecord> myBorrows(Integer userId, String status, String keyword);

    List<BorrowRecord> allBorrows(String keyword, String status);

    Map<String, String> getBorrowRules();
}
