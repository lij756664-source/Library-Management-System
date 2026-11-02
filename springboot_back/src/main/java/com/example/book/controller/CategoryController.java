package com.example.book.controller;

import com.example.book.annotation.RequireRole;
import com.example.book.common.Result;
import com.example.book.entity.Category;
import com.example.book.service.ICategoryService;
import com.example.book.service.IOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 图书分类设置接口
 * <p>查询对全部登录用户开放；新增、修改、删除分类属于后台数据维护，仅管理员可操作，
 * 由 {@link RequireRole} + JwtInterceptor 完成角色校验，读者无法越权访问。</p>
 * <p>分类的增删改均写入操作日志（操作人、操作时间、操作内容），做到操作留痕、行为可追溯。</p>
 */
@RestController
@RequestMapping("/api/categories")
@RequireRole({"ADMIN", "USER"})
public class CategoryController {

    private final ICategoryService categoryService;
    private final IOperationLogService operationLogService;

    public CategoryController(ICategoryService categoryService, IOperationLogService operationLogService) {
        this.categoryService = categoryService;
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public Result<List<Category>> list() {
        return Result.success(categoryService.list());
    }

    /** 【管理员】新增图书分类 */
    @PostMapping
    @RequireRole("ADMIN")
    public Result<Void> add(HttpServletRequest request, @RequestBody Category category) {
        categoryService.add(category);
        operationLogService.record(currentUserId(request), currentUsername(request), "新增图书分类",
                "新增分类《" + category.getName() + "》，描述：" + displayOrDash(category.getDescription()));
        return Result.success("添加成功", null);
    }

    /** 【管理员】修改图书分类 */
    @PutMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<Void> update(HttpServletRequest request, @PathVariable Integer id, @RequestBody Category category) {
        category.setId(id);
        categoryService.update(category);
        operationLogService.record(currentUserId(request), currentUsername(request), "修改图书分类",
                "修改分类（ID：" + id + "）为《" + category.getName() + "》，描述："
                        + displayOrDash(category.getDescription()));
        return Result.success("更新成功", null);
    }

    /** 【管理员】删除图书分类（分类下仍有图书时会被拦截） */
    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<Void> delete(HttpServletRequest request, @PathVariable Integer id) {
        categoryService.delete(id);
        operationLogService.record(currentUserId(request), currentUsername(request), "删除图书分类",
                "删除图书分类（ID：" + id + "）");
        return Result.success("删除成功", null);
    }

    /** 日志展示用：空值统一显示为“未设置”，避免日志内容出现 null */
    private String displayOrDash(String value) {
        return value == null || value.isBlank() ? "未设置" : value;
    }

    private Integer currentUserId(HttpServletRequest request) {
        return (Integer) request.getAttribute("userId");
    }

    private String currentUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }
}
