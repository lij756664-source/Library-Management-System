package com.example.book.service;

import com.example.book.common.BusinessException;
import com.example.book.entity.Book;
import com.example.book.entity.Category;
import com.example.book.mapper.BookMapper;
import com.example.book.mapper.BorrowMapper;
import com.example.book.mapper.CategoryMapper;
import com.example.book.mapper.ReservationMapper;
import com.example.book.service.impl.BookServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 图书资源管理模块 - 异常处理单元测试
 * <p>覆盖新书入库、修改图书、副本维护、图书下架、图书检索、详情查询等场景的校验与业务拦截，
 * 保证非法输入被拦截并返回友好提示，而不是抛出数据库异常或产生脏数据。</p>
 */
@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookMapper bookMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private BorrowMapper borrowMapper;
    @Mock
    private ReservationMapper reservationMapper;

    @InjectMocks
    private BookServiceImpl bookService;

    /** 构造一份合法的入库图书数据 */
    private Book validBook() {
        Book book = new Book();
        book.setTitle("Java编程思想");
        book.setAuthor("Bruce Eckel");
        book.setIsbn("978-7-111-21382-6");
        book.setCategoryId(1);
        book.setPrice(new BigDecimal("108.00"));
        book.setStock(5);
        book.setLocation("三楼计算机阅览室 A-01 书架");
        book.setDescription("Java经典教材");
        return book;
    }

    private Category category(Integer id) {
        Category category = new Category();
        category.setId(id);
        category.setName("计算机");
        return category;
    }

    // ---------------- 新书入库 ----------------

    @Test
    @DisplayName("新书入库：ISBN 已存在时禁止重复入库")
    void addShouldRejectDuplicateIsbn() {
        Book book = validBook();
        when(categoryMapper.findById(1)).thenReturn(category(1));
        when(bookMapper.countByIsbn(book.getIsbn(), null)).thenReturn(1);

        BusinessException e = assertThrows(BusinessException.class, () -> bookService.add(book));
        assertTrue(e.getMessage().contains("已存在"));
        verify(bookMapper, never()).insert(any(Book.class));
    }

    @Test
    @DisplayName("新书入库：必填项为空时拦截脏数据")
    void addShouldRejectBlankTitle() {
        Book book = validBook();
        book.setTitle("   ");

        BusinessException e = assertThrows(BusinessException.class, () -> bookService.add(book));
        assertTrue(e.getMessage().contains("书名"));
        verify(bookMapper, never()).insert(any(Book.class));
    }

    @Test
    @DisplayName("新书入库：库存为负数时拦截")
    void addShouldRejectNegativeStock() {
        Book book = validBook();
        book.setStock(-1);
        when(categoryMapper.findById(1)).thenReturn(category(1));

        BusinessException e = assertThrows(BusinessException.class, () -> bookService.add(book));
        assertTrue(e.getMessage().contains("不能为负数"));
        verify(bookMapper, never()).insert(any(Book.class));
    }

    @Test
    @DisplayName("新书入库：分类编码无效时拦截")
    void addShouldRejectInvalidCategory() {
        Book book = validBook();
        when(categoryMapper.findById(1)).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class, () -> bookService.add(book));
        assertTrue(e.getMessage().contains("分类"));
        verify(bookMapper, never()).insert(any(Book.class));
    }

    @Test
    @DisplayName("新书入库：ISBN 格式非法时拦截")
    void addShouldRejectInvalidIsbnFormat() {
        Book book = validBook();
        book.setIsbn("abc123");
        when(categoryMapper.findById(1)).thenReturn(category(1));

        BusinessException e = assertThrows(BusinessException.class, () -> bookService.add(book));
        assertTrue(e.getMessage().contains("ISBN"));
        verify(bookMapper, never()).insert(any(Book.class));
    }

    @Test
    @DisplayName("新书入库：合法数据写入成功并清理首尾空白")
    void addShouldSucceedWithValidData() {
        Book book = validBook();
        book.setTitle("  Java编程思想  ");
        book.setLocation("  三楼计算机阅览室 A-01 书架  ");
        when(categoryMapper.findById(1)).thenReturn(category(1));
        when(bookMapper.countByIsbn(book.getIsbn(), null)).thenReturn(0);
        when(bookMapper.insert(book)).thenReturn(1);

        bookService.add(book);

        assertEquals("Java编程思想", book.getTitle());
        assertEquals("三楼计算机阅览室 A-01 书架", book.getLocation());
        verify(bookMapper).insert(book);
    }

    // ---------------- 修改图书信息 ----------------

    @Test
    @DisplayName("修改图书：目标图书不存在时给出友好提示")
    void updateShouldRejectMissingBook() {
        Book book = validBook();
        book.setId(999);
        when(bookMapper.findById(999)).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class, () -> bookService.update(book));
        assertEquals(404, e.getCode());
        assertTrue(e.getMessage().contains("不存在"));
        verify(bookMapper, never()).update(any(Book.class));
    }

    @Test
    @DisplayName("修改图书：ISBN 被其他图书占用时拦截")
    void updateShouldRejectDuplicatedIsbn() {
        Book book = validBook();
        book.setId(1);
        when(bookMapper.findById(1)).thenReturn(validBook());
        when(categoryMapper.findById(1)).thenReturn(category(1));
        when(bookMapper.countByIsbn(book.getIsbn(), 1)).thenReturn(1);

        BusinessException e = assertThrows(BusinessException.class, () -> bookService.update(book));
        assertTrue(e.getMessage().contains("占用"));
        verify(bookMapper, never()).update(any(Book.class));
    }

    // ---------------- 馆藏副本维护 ----------------

    @Test
    @DisplayName("副本维护：减到负数时拦截，库存不允许小于 0")
    void adjustStockShouldRejectNegativeResult() {
        Book book = validBook();
        book.setStock(0);
        when(bookMapper.findById(1)).thenReturn(book);

        BusinessException e = assertThrows(BusinessException.class, () -> bookService.adjustStock(1, -1));
        assertTrue(e.getMessage().contains("不能小于 0"));
        verify(bookMapper, never()).adjustStock(anyInt(), anyInt());
    }

    @Test
    @DisplayName("副本维护：调整数量为 0 或为空时拦截")
    void adjustStockShouldRejectZeroDelta() {
        assertThrows(BusinessException.class, () -> bookService.adjustStock(1, 0));
        assertThrows(BusinessException.class, () -> bookService.adjustStock(1, null));
    }

    @Test
    @DisplayName("副本维护：合法增量返回调整后的库存")
    void adjustStockShouldReturnNewStock() {
        Book book = validBook();
        book.setStock(5);
        when(bookMapper.findById(1)).thenReturn(book);
        when(bookMapper.adjustStock(1, 2)).thenReturn(1);

        assertEquals(7, bookService.adjustStock(1, 2));
    }

    // ---------------- 图书下架 ----------------

    @Test
    @DisplayName("图书下架：存在未归还借阅记录时拦截，防止业务出错")
    void deleteShouldRejectWhenBorrowingRecordsExist() {
        when(bookMapper.findById(1)).thenReturn(validBook());
        when(borrowMapper.countActiveByBookId(1)).thenReturn(2);

        BusinessException e = assertThrows(BusinessException.class, () -> bookService.delete(1));
        assertTrue(e.getMessage().contains("未完成的借阅记录"));
        verify(bookMapper, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("图书下架：存在排队预约时拦截")
    void deleteShouldRejectWhenReservationsExist() {
        when(bookMapper.findById(1)).thenReturn(validBook());
        when(borrowMapper.countActiveByBookId(1)).thenReturn(0);
        when(reservationMapper.countActiveByBookId(1)).thenReturn(1);

        BusinessException e = assertThrows(BusinessException.class, () -> bookService.delete(1));
        assertTrue(e.getMessage().contains("预约"));
        verify(bookMapper, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("图书下架：图书不存在时给出友好提示")
    void deleteShouldRejectMissingBook() {
        when(bookMapper.findById(1)).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class, () -> bookService.delete(1));
        assertEquals(404, e.getCode());
        verify(bookMapper, never()).deleteById(anyInt());
    }

    // ---------------- 读者端：检索与详情 ----------------

    @Test
    @DisplayName("高级搜索：关键词含 Like 通配符等特殊字符时返回友好提示而不是崩溃")
    void listShouldRejectKeywordWithSpecialCharacters() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> bookService.list("%_\\", null, null, 2));
        assertTrue(e.getMessage().contains("特殊字符"));
    }

    @Test
    @DisplayName("高级搜索：关键词超长时提示，空关键词等价于不筛选")
    void listShouldHandleKeywordLengthAndBlank() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> bookService.list("a".repeat(101), null, null, 2));
        assertTrue(e.getMessage().contains("长度"));

        List<Book> expected = Collections.singletonList(validBook());
        when(bookMapper.findAll(isNull(), isNull(), isNull(), eq(2))).thenReturn(expected);
        assertEquals(expected, bookService.list("   ", null, null, 2));
    }

    @Test
    @DisplayName("高级搜索：非法分类参数被拦截")
    void listShouldRejectInvalidCategoryId() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> bookService.list(null, 0, null, 2));
        assertTrue(e.getMessage().contains("分类"));
    }

    @Test
    @DisplayName("高级搜索：仅显示可借时把 availableOnly 透传给查询")
    void listShouldPassAvailableOnly() {
        when(bookMapper.findAll(null, 1, true, 2)).thenReturn(Collections.emptyList());

        assertEquals(Collections.emptyList(), bookService.list(null, 1, true, 2));
        verify(bookMapper).findAll(isNull(), eq(1), anyBoolean(), eq(2));
    }

    @Test
    @DisplayName("图书详情：编号非法或图书不存在时提示图书不存在")
    void getByIdShouldRejectMissingBook() {
        assertThrows(BusinessException.class, () -> bookService.getById(null));
        assertThrows(BusinessException.class, () -> bookService.getById(0));
        when(bookMapper.findById(100)).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class, () -> bookService.getById(100));
        assertEquals(404, e.getCode());
        assertTrue(e.getMessage().contains("不存在"));
    }

    @Test
    @DisplayName("图书详情：正常返回馆藏位置等公开馆藏信息")
    void getByIdShouldReturnBookWithLocation() {
        when(bookMapper.findById(1)).thenReturn(validBook());

        Book book = bookService.getById(1);
        assertEquals("三楼计算机阅览室 A-01 书架", book.getLocation());
        assertNull(book.getCreateTime());
    }
}
