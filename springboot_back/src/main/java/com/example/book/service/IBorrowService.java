package com.example.book.service;

import com.example.book.entity.BorrowRecord;
import com.example.book.vo.BookStatVO;
import com.example.book.vo.CategoryStatVO;
import com.example.book.vo.DashboardVO;

import java.util.List;
import java.util.Map;

public interface IBorrowService {

    void borrowBook(Integer userId, Integer bookId);

    void renewBook(Integer userId, Integer borrowId);

    void returnBook(Integer userId, Integer borrowId, boolean isAdmin);

    List<BorrowRecord> myBorrows(Integer userId, String status, String keyword);

    List<BorrowRecord> allBorrows(String keyword, String status);

    Map<String, String> getBorrowRules();

    DashboardVO dashboard();

    List<CategoryStatVO> statByCategory();

    List<BookStatVO> statTopBooks(int limit);
}
