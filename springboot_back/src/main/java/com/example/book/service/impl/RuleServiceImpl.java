package com.example.book.service.impl;

import com.example.book.service.IRuleService;
import com.example.book.common.BusinessException;
import com.example.book.entity.BorrowRecord;
import com.example.book.entity.SystemConfig;
import com.example.book.mapper.SystemConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RuleServiceImpl implements IRuleService {
    private static final List<String> KEYS = List.of(
            "borrow_days", "max_renew_count", "renew_days", "fine_per_day", "max_borrow_count");
    private static final List<String> NAMES = List.of(
            "借阅期限", "续借次数上限", "每次续借天数", "每日罚金", "同时借阅上限");
    private final SystemConfigMapper mapper;

    public RuleServiceImpl(SystemConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Map<String, String> current() {
        // 单条查询取得完整规则；调用方可复用结果，避免分次查询混用版本。
        Map<String, String> result = new LinkedHashMap<>();
        for (SystemConfig config : mapper.findAll()) {
            if (KEYS.contains(config.getConfigKey())) {
                result.put(config.getConfigKey(), config.getConfigValue());
            }
        }
        if (result.size() != KEYS.size()) {
            throw new BusinessException("借阅规则暂不可用，请联系管理员");
        }
        return result;
    }

    // 仅保存规则时锁定整组配置，协调并发保存，锁持有至事务结束。
    private void lock() {
        if (mapper.lockRules().size() != KEYS.size()) {
            throw new BusinessException("借阅规则暂不可用，请联系管理员");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> save(Map<String, String> input) {
        Map<String, String> values = new LinkedHashMap<>();
        int[] minimum = {1, 0, 1, 0, 1};
        int[] maximum = {90, 5, 30, 5, 10};
        for (int index = 0; index < KEYS.size(); index++) {
            String key = KEYS.get(index);
            String name = NAMES.get(index);
            String value = input.get(key);
            if (value == null || value.isBlank()) {
                throw new BusinessException("请填写" + name);
            }
            boolean money = index == 3;
            if (!value.matches(money ? "[0-9]+(\\.[0-9]{1,2})?" : "[0-9]+")) {
                throw new BusinessException(name + (money ? "须为非负数，且最多保留两位小数" : "须为整数"));
            }
            BigDecimal number = new BigDecimal(value);
            if (number.compareTo(BigDecimal.valueOf(minimum[index])) < 0
                    || number.compareTo(BigDecimal.valueOf(maximum[index])) > 0) {
                throw new BusinessException(name + "的范围为" + minimum[index] + "—"
                        + (money ? "5.00元" : maximum[index]));
            }
            values.put(key, money ? number.setScale(2).toPlainString() : number.toPlainString());
        }
        // 所有输入校验通过后再加锁，五项更新在同一事务中提交。
        lock();
        values.forEach((key, value) -> {
            if (mapper.updateValue(key, value) != 1) {
                throw new BusinessException("规则保存失败，请重新查询当前规则");
            }
        });
        return current();
    }

    // 保留借阅当时的规则，后续续借和罚金不随新规则变化。
    @Override
    public void snapshot(BorrowRecord record, Map<String, String> rules) {
        record.setBorrowDaysSnapshot(Integer.valueOf(rules.get("borrow_days")));
        record.setMaxRenewCountSnapshot(Integer.valueOf(rules.get("max_renew_count")));
        record.setRenewDaysSnapshot(Integer.valueOf(rules.get("renew_days")));
        record.setFinePerDaySnapshot(new BigDecimal(rules.get("fine_per_day")));
    }
}
