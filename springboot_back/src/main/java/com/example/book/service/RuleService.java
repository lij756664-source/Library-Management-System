package com.example.book.service;

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
public class RuleService {
    public static final List<String> KEYS = List.of(
            "borrow_days", "max_renew_count", "renew_days", "fine_per_day", "max_borrow_count");
    private static final List<String> NAMES = List.of(
            "借阅期限", "续借次数上限", "每次续借天数", "每日罚金", "同时借阅上限");
    private final SystemConfigMapper mapper;

    public RuleService(SystemConfigMapper mapper) {
        this.mapper = mapper;
    }

    public Map<String, String> current() {
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

    // 与借出、续借、归还按相同顺序加锁，锁持有至外层事务结束。
    public void lock() {
        if (mapper.lockRules().size() != KEYS.size()) {
            throw new BusinessException("借阅规则暂不可用，请联系管理员");
        }
    }

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
        lock();
        values.forEach((key, value) -> {
            if (mapper.updateValue(key, value) != 1) {
                throw new BusinessException("规则保存失败，请重新查询当前规则");
            }
        });
        return current();
    }

    public void snapshot(BorrowRecord record, Map<String, String> rules) {
        record.setBorrowDaysSnapshot(Integer.valueOf(rules.get("borrow_days")));
        record.setMaxRenewCountSnapshot(Integer.valueOf(rules.get("max_renew_count")));
        record.setRenewDaysSnapshot(Integer.valueOf(rules.get("renew_days")));
        record.setFinePerDaySnapshot(new BigDecimal(rules.get("fine_per_day")));
    }
}
