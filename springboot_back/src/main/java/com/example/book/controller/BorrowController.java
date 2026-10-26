package com.example.book.controller;

import com.example.book.annotation.RequireRole;
import com.example.book.common.Result;
import com.example.book.entity.BorrowRecord;
import com.example.book.service.IBorrowService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/borrows")
@RequireRole({"ADMIN", "USER"})
public class BorrowController {

    private final IBorrowService borrowService;

    public BorrowController(IBorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @PostMapping
    public Result<Void> borrow(HttpServletRequest request, @RequestBody Map<String, Integer> body) {
        Integer bookId = body.get("bookId");
        if (bookId == null) {
            return Result.error(400, "图书ID不能为空");
        }
        Integer userId = (Integer) request.getAttribute("userId");
        borrowService.borrowBook(userId, bookId);
        return Result.success("借阅成功", null);
    }

    @PutMapping("/{id}/renew")
    public Result<Void> renew(HttpServletRequest request, @PathVariable Integer id) {
        Integer userId = (Integer) request.getAttribute("userId");
        borrowService.renewBook(userId, id);
        return Result.success("续借成功", null);
    }

    @PutMapping("/{id}/return")
    public Result<Void> returnBook(HttpServletRequest request, @PathVariable Integer id) {
        Integer userId = (Integer) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        borrowService.returnBook(userId, id, "ADMIN".equals(role));
        return Result.success("归还成功", null);
    }

    @GetMapping("/my")
    public Result<List<BorrowRecord>> myBorrows(
            HttpServletRequest request,
            @RequestParam(required = false) String status) {
        Integer userId = (Integer) request.getAttribute("userId");
        return Result.success(borrowService.myBorrows(userId, status));
    }

    @GetMapping
    @RequireRole("ADMIN")
    public Result<List<BorrowRecord>> allBorrows(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.success(borrowService.allBorrows(keyword, status));
    }

    @GetMapping("/rules")
    public Result<Map<String, String>> rules() {
        return Result.success(borrowService.getBorrowRules());
    }
}
