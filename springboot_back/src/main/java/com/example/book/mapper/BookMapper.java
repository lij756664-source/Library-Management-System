package com.example.book.mapper;

import com.example.book.entity.Book;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BookMapper {

    List<Book> findAll(@Param("keyword") String keyword,
                       @Param("categoryId") Integer categoryId,
                       @Param("userId") Integer userId);

    Book findById(@Param("id") Integer id);

    int insert(Book book);

    int update(Book book);

    int deleteById(@Param("id") Integer id);

    int decreaseStock(@Param("id") Integer id);

    int increaseStock(@Param("id") Integer id);
}
