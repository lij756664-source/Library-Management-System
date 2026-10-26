package com.example.book.mapper;

import com.example.book.entity.Category;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CategoryMapper {

    List<Category> findAll();

    Category findById(@Param("id") Integer id);

    Category findByName(@Param("name") String name);

    int insert(Category category);

    int update(Category category);

    int deleteById(@Param("id") Integer id);

    int countBooksByCategoryId(@Param("categoryId") Integer categoryId);
}
