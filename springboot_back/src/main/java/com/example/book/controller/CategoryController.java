package com.example.book.controller;

import com.example.book.annotation.RequireRole;
import com.example.book.common.Result;
import com.example.book.entity.Category;
import com.example.book.service.ICategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequireRole({"ADMIN", "USER"})
public class CategoryController {

    private final ICategoryService categoryService;

    public CategoryController(ICategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public Result<List<Category>> list() {
        return Result.success(categoryService.list());
    }

    @PostMapping
    @RequireRole("ADMIN")
    public Result<Void> add(@RequestBody Category category) {
        categoryService.add(category);
        return Result.success("添加成功", null);
    }

    @PutMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<Void> update(@PathVariable Integer id, @RequestBody Category category) {
        category.setId(id);
        categoryService.update(category);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<Void> delete(@PathVariable Integer id) {
        categoryService.delete(id);
        return Result.success("删除成功", null);
    }
}
