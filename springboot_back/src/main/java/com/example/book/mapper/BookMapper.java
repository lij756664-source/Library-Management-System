package com.example.book.mapper;

import com.example.book.entity.Book;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BookMapper {

    /**
     * 馆藏检索
     *
     * @param keyword       书名/作者/ISBN 模糊关键词，null 表示不筛选
     * @param categoryId    分类ID，null 表示全部分类
     * @param availableOnly true 表示只查询当前有可借库存的图书
     * @param userId        当前登录用户ID，用于返回本人预约状态
     */
    List<Book> findAll(@Param("keyword") String keyword,
                       @Param("categoryId") Integer categoryId,
                       @Param("availableOnly") Boolean availableOnly,
                       @Param("userId") Integer userId);

    Book findById(@Param("id") Integer id);

    int insert(Book book);

    int update(Book book);

    int deleteById(@Param("id") Integer id);

    int decreaseStock(@Param("id") Integer id);

    int increaseStock(@Param("id") Integer id);

    /** 统计同 ISBN 的图书数量（新增时 excludeId 传 null；修改时排除自身） */
    int countByIsbn(@Param("isbn") String isbn, @Param("excludeId") Integer excludeId);

    /** 按增量调整库存，库存不足时不会更新（返回0），保证库存不为负 */
    int adjustStock(@Param("id") Integer id, @Param("delta") int delta);
}
