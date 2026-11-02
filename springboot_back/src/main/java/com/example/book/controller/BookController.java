package com.example.book.controller;

import com.example.book.annotation.RequireRole;
import com.example.book.common.Result;
import com.example.book.dto.StockDTO;
import com.example.book.entity.Book;
import com.example.book.service.IBookService;
import com.example.book.service.IOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 图书资源管理接口
 * <p>查询类接口对全部登录用户开放；新增、修改、库存维护、下架等后台接口仅对管理员开放，
 * 由 {@link RequireRole} + JwtInterceptor 完成角色校验，读者无法越权访问。</p>
 * <p>所有后台写操作均写入操作日志（操作账号、时间、内容），实现操作留痕、行为可追溯。</p>
 */
@RestController
@RequestMapping("/api/books")
@RequireRole({"ADMIN", "USER"})
public class BookController {

    /** 读者视图下不对外暴露的系统内部字段 */
    private static final String ROLE_ADMIN = "ADMIN";

    private final IBookService bookService;
    private final IOperationLogService operationLogService;

    public BookController(IBookService bookService, IOperationLogService operationLogService) {
        this.bookService = bookService;
        this.operationLogService = operationLogService;
    }

    /**
     * 图书检索（读者/管理员共用）
     * 关键词为空或纯空格时不加筛选；超长与特殊字符参数由服务层做过滤与友好提示
     *
     * @param availableOnly true 表示只返回当前有可借库存的图书（高级搜索-仅显示可借）
     */
    @GetMapping
    public Result<List<Book>> list(
            HttpServletRequest request,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Boolean availableOnly) {
        Integer userId = (Integer) request.getAttribute("userId");
        List<Book> books = bookService.list(keyword, categoryId, availableOnly, userId);
        // 合规展示：读者端仅返回公开馆藏信息，屏蔽系统内部管理字段
        if (!ROLE_ADMIN.equals(currentRole(request))) {
            books.forEach(this::maskInternalFields);
        }
        return Result.success(books);
    }

    /**
     * 图书详情（读者可查看基础馆藏信息与可借状态）
     * 图书不存在/已下架/编号非法统一返回友好提示，不暴露数据库异常
     */
    @GetMapping("/{id}")
    public Result<Book> getById(HttpServletRequest request, @PathVariable Integer id) {
        Book book = bookService.getById(id);
        if (!ROLE_ADMIN.equals(currentRole(request))) {
            maskInternalFields(book);
        }
        return Result.success(book);
    }

    /** 【管理员】新书入库 */
    @PostMapping
    @RequireRole("ADMIN")
    public Result<Void> add(HttpServletRequest request, @RequestBody Book book) {
        bookService.add(book);
        operationLogService.record(currentUserId(request), currentUsername(request), "图书入库",
                "新增图书《" + book.getTitle() + "》，ISBN：" + book.getIsbn()
                        + "，分类ID：" + book.getCategoryId() + "，入库库存：" + book.getStock()
                        + " 册，馆藏位置：" + displayOrDash(book.getLocation()));
        return Result.success("添加成功", null);
    }

    /** 【管理员】修改图书信息 */
    @PutMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<Void> update(HttpServletRequest request, @PathVariable Integer id, @RequestBody Book book) {
        book.setId(id);
        bookService.update(book);
        operationLogService.record(currentUserId(request), currentUsername(request), "修改图书信息",
                "修改图书《" + book.getTitle() + "》（ID：" + id + "，ISBN：" + book.getIsbn()
                        + "），库存：" + book.getStock() + " 册，馆藏位置：" + displayOrDash(book.getLocation()));
        return Result.success("更新成功", null);
    }

    /**
     * 【管理员】馆藏副本（库存）增减维护
     * 请求体：{ "delta": 1 } 增加一册，{ "delta": -1 } 减少一册；库存最小值为 0
     */
    @PutMapping("/{id}/stock")
    @RequireRole("ADMIN")
    public Result<Integer> adjustStock(HttpServletRequest request,
                                       @PathVariable Integer id,
                                       @RequestBody StockDTO stockDTO) {
        Integer delta = stockDTO == null ? null : stockDTO.getDelta();
        // 先取出书名用于审计日志，图书不存在时由服务层统一返回友好提示
        Book book = bookService.getById(id);
        int newStock = bookService.adjustStock(id, delta);
        operationLogService.record(currentUserId(request), currentUsername(request), "维护馆藏副本",
                "图书ID：" + id + "（《" + book.getTitle() + "》，ISBN：" + book.getIsbn() + "），库存调整："
                        + (delta > 0 ? "+" : "") + delta + "，调整后库存：" + newStock + " 册");
        return Result.success("库存调整成功", newStock);
    }

    /** 【管理员】图书下架（存在未完成借阅/排队预约时会被拦截） */
    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<Void> delete(HttpServletRequest request, @PathVariable Integer id) {
        // 先取出书名等信息用于审计日志留痕，图书不存在时由服务层返回友好提示
        Book target = bookService.getById(id);
        bookService.delete(id);
        operationLogService.record(currentUserId(request), currentUsername(request), "图书下架",
                "下架并删除图书《" + target.getTitle() + "》（ID：" + id + "，ISBN：" + target.getIsbn()
                        + "，下架前可借库存：" + target.getStock() + " 册）");
        return Result.success("下架成功", null);
    }

    /** 日志展示用：空值统一显示为“未设置”，避免日志内容出现 null */
    private String displayOrDash(String value) {
        return value == null || value.isBlank() ? "未设置" : value;
    }

    /** 读者视图：屏蔽创建/更新时间等后台管理字段 */
    private void maskInternalFields(Book book) {
        if (book == null) {
            return;
        }
        book.setCreateTime(null);
        book.setUpdateTime(null);
    }

    private Integer currentUserId(HttpServletRequest request) {
        return (Integer) request.getAttribute("userId");
    }

    private String currentUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }

    private String currentRole(HttpServletRequest request) {
        return (String) request.getAttribute("role");
    }
}
