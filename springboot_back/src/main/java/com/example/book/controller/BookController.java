package com.example.book.controller;

import com.example.book.annotation.RequireRole;
import com.example.book.common.Result;
import com.example.book.entity.Book;
import com.example.book.service.IBookService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequireRole({"ADMIN", "USER"})
public class BookController {

    private final IBookService bookService;

    public BookController(IBookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public Result<List<Book>> list(
            HttpServletRequest request,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId) {
        Integer userId = (Integer) request.getAttribute("userId");
        return Result.success(bookService.list(keyword, categoryId, userId));
    }

    @GetMapping("/{id}")
    public Result<Book> getById(@PathVariable Integer id) {
        return Result.success(bookService.getById(id));
    }

    @PostMapping
    @RequireRole("ADMIN")
    public Result<Void> add(@RequestBody Book book) {
        bookService.add(book);
        return Result.success("添加成功", null);
    }

    @PutMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<Void> update(@PathVariable Integer id, @RequestBody Book book) {
        book.setId(id);
        bookService.update(book);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<Void> delete(@PathVariable Integer id) {
        bookService.delete(id);
        return Result.success("删除成功", null);
    }
}
