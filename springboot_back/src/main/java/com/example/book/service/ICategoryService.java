package com.example.book.service;

import com.example.book.entity.Category;

import java.util.List;

public interface ICategoryService {

    List<Category> list();

    void add(Category category);

    void update(Category category);

    void delete(Integer id);
}
