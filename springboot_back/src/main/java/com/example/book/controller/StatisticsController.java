package com.example.book.controller;

import com.example.book.annotation.RequireRole;
import com.example.book.common.Result;
import com.example.book.service.IBorrowService;
import com.example.book.vo.BookStatVO;
import com.example.book.vo.CategoryStatVO;
import com.example.book.vo.DashboardVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequireRole("ADMIN")
public class StatisticsController {

    private final IBorrowService borrowService;

    public StatisticsController(IBorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.success(borrowService.dashboard());
    }

    @GetMapping("/category")
    public Result<List<CategoryStatVO>> byCategory() {
        return Result.success(borrowService.statByCategory());
    }

    @GetMapping("/top-books")
    public Result<List<BookStatVO>> topBooks(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(borrowService.statTopBooks(limit));
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> data = new HashMap<>();
        data.put("dashboard", borrowService.dashboard());
        data.put("categoryStats", borrowService.statByCategory());
        data.put("topBooks", borrowService.statTopBooks(5));
        return Result.success(data);
    }
}
