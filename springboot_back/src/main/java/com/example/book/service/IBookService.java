package com.example.book.service;

import com.example.book.entity.Book;

import java.util.List;

public interface IBookService {

    /**
     * 馆藏检索
     *
     * @param keyword       书名/作者/ISBN 模糊关键词，空或纯空格表示不筛选
     * @param categoryId    分类ID，null 表示全部分类
     * @param availableOnly 是否只查询当前可借（库存大于 0）的图书，null 等同 false
     * @param userId        当前登录用户ID，用于返回本人预约状态
     */
    List<Book> list(String keyword, Integer categoryId, Boolean availableOnly, Integer userId);

    Book getById(Integer id);

    void add(Book book);

    void update(Book book);

    void delete(Integer id);

    /**
     * 馆藏副本（库存）增减维护
     *
     * @param id    图书ID
     * @param delta 增量：正数增加副本，负数减少副本
     * @return 调整后的库存数量
     */
    int adjustStock(Integer id, Integer delta);
}
