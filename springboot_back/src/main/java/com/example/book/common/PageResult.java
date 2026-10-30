package com.example.book.common;

import lombok.Data;

import java.util.List;

/**
 * 分页查询统一返回结构
 */
@Data
public class PageResult<T> {

    /** 总记录数 */
    private long total;
    /** 当前页数据 */
    private List<T> list;

    public PageResult() {
    }

    public PageResult(long total, List<T> list) {
        this.total = total;
        this.list = list;
    }

    public static <T> PageResult<T> of(long total, List<T> list) {
        return new PageResult<>(total, list);
    }
}
