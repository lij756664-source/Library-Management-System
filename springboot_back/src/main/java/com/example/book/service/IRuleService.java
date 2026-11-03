package com.example.book.service;

import com.example.book.entity.BorrowRecord;
import java.util.Map;

/** 借阅规则查询、整组保存及历史快照。 */
public interface IRuleService {
    Map<String, String> current();

    Map<String, String> save(Map<String, String> input);

    /** 使用调用方已读取的规则生成快照，不再次查询数据库。 */
    void snapshot(BorrowRecord record, Map<String, String> rules);
}
