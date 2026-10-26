package com.example.book.service.impl;

import com.example.book.common.BusinessException;
import com.example.book.entity.Book;
import com.example.book.mapper.BookMapper;
import com.example.book.mapper.BorrowMapper;
import com.example.book.mapper.CategoryMapper;
import com.example.book.service.IBookService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookServiceImpl implements IBookService {

    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;
    private final BorrowMapper borrowMapper;

    public BookServiceImpl(BookMapper bookMapper, CategoryMapper categoryMapper, BorrowMapper borrowMapper) {
        this.bookMapper = bookMapper;
        this.categoryMapper = categoryMapper;
        this.borrowMapper = borrowMapper;
    }

    @Override
    public List<Book> list(String keyword, Integer categoryId) {
        return bookMapper.findAll(keyword, categoryId);
    }

    @Override
    public Book getById(Integer id) {
        Book book = bookMapper.findById(id);
        if (book == null) {
            throw new BusinessException(404, "图书不存在");
        }
        return book;
    }

    @Override
    public void add(Book book) {
        validateBook(book);
        if (categoryMapper.findById(book.getCategoryId()) == null) {
            throw new BusinessException(400, "分类不存在");
        }
        if (bookMapper.insert(book) <= 0) {
            throw new BusinessException("添加图书失败");
        }
    }

    @Override
    public void update(Book book) {
        validateBook(book);
        if (bookMapper.findById(book.getId()) == null) {
            throw new BusinessException(404, "图书不存在");
        }
        if (categoryMapper.findById(book.getCategoryId()) == null) {
            throw new BusinessException(400, "分类不存在");
        }
        if (bookMapper.update(book) <= 0) {
            throw new BusinessException("更新图书失败");
        }
    }

    @Override
    public void delete(Integer id) {
        if (bookMapper.findById(id) == null) {
            throw new BusinessException(404, "图书不存在");
        }
        if (borrowMapper.countByBookId(id) > 0) {
            throw new BusinessException(400, "该图书存在借阅记录，无法删除");
        }
        if (bookMapper.deleteById(id) <= 0) {
            throw new BusinessException("删除图书失败");
        }
    }

    private void validateBook(Book book) {
        if (book.getTitle() == null || book.getTitle().isBlank()) {
            throw new BusinessException(400, "书名不能为空");
        }
        if (book.getTitle().length() > 200) {
            throw new BusinessException(400, "书名长度不能超过200字符");
        }
        if (book.getCategoryId() == null) {
            throw new BusinessException(400, "请选择分类");
        }
        if (book.getStock() != null && book.getStock() < 0) {
            throw new BusinessException(400, "库存不能为负数");
        }
        if (book.getPrice() != null && book.getPrice().signum() < 0) {
            throw new BusinessException(400, "价格不能为负数");
        }
    }
}
