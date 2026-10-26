package com.example.book.service;

import com.example.book.entity.Book;

import java.util.List;

public interface IBookService {

    List<Book> list(String keyword, Integer categoryId);

    Book getById(Integer id);

    void add(Book book);

    void update(Book book);

    void delete(Integer id);
}
