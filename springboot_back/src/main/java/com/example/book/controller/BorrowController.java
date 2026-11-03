package com.example.book.controller;

import com.example.book.annotation.RequireRole;
import com.example.book.common.Result;
import com.example.book.entity.BorrowRecord;
import com.example.book.service.IBorrowService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 借阅相关接口：借阅 / 续借 / 归还 / 记录查询
 * 前端 vue_front/src/api/borrow.js 与本类路径一一对应
 */
@RestController
@RequestMapping("/api/borrows")
@RequireRole({"ADMIN", "USER"})
public class BorrowController {

    private final IBorrowService borrowService;

    public BorrowController(IBorrowService borrowService) {
        this.borrowService = borrowService;
    }

    /** 借阅：POST /api/borrows，body 只传 bookId，借书人从 JWT 解析，防止前端伪造 userId */
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

    /** 续借：PUT /api/borrows/{id}/renew，只能续自己的、未逾期记录 */
    @PutMapping("/{id}/renew")
    public Result<Void> renew(HttpServletRequest request, @PathVariable Integer id) {
        Integer userId = (Integer) request.getAttribute("userId");
        borrowService.renewBook(userId, id);
        return Result.success("续借成功", null);
    }

    /** 归还：PUT /api/borrows/{id}/return；管理员可代还 */
    @PutMapping("/{id}/return")
    public Result<Void> returnBook(HttpServletRequest request, @PathVariable Integer id) {
        Integer userId = (Integer) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        borrowService.returnBook(userId, id, "ADMIN".equals(role));
        return Result.success("归还成功", null);
    }

    /** 读者借阅记录：GET /api/borrows/my */
    @GetMapping("/my")
    public Result<List<BorrowRecord>> myBorrows(
            HttpServletRequest request,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Integer userId = (Integer) request.getAttribute("userId");
        return Result.success(borrowService.myBorrows(userId, status, keyword));
    }

    /** 管理员借阅记录：GET /api/borrows，仅 ADMIN */
    @GetMapping
    @RequireRole("ADMIN")
    public Result<List<BorrowRecord>> allBorrows(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.success(borrowService.allBorrows(keyword, status));
    }

    /** 借阅规则（借期、续借次数、罚款等） */
    @GetMapping("/rules")
    public Result<Map<String, String>> rules() {
        return Result.success(borrowService.getBorrowRules());
    }
}
