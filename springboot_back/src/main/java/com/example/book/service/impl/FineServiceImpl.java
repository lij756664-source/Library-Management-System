package com.example.book.service.impl;

import com.example.book.service.IFineService;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class FineServiceImpl implements IFineService {
    @Override
    public Result calculate(LocalDate due, BigDecimal daily, LocalDate date) {
        // 自然日差小于等于零时不逾期；零日费仍保留实际逾期天数。
        int days = Math.toIntExact(Math.max(0, ChronoUnit.DAYS.between(due, date)));
        BigDecimal amount = daily.multiply(BigDecimal.valueOf(days)).setScale(2);
        return new Result(days, amount);
    }
}
