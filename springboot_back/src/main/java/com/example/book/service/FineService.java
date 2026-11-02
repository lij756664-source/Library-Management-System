package com.example.book.service;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
@Service
public class FineService {
 public static final ZoneId ZONE=ZoneId.of("Asia/Shanghai");
 public record Result(int overdueDays, BigDecimal amount) {}
 public Result calculate(LocalDate due, BigDecimal daily, LocalDate date) { int days=Math.toIntExact(Math.max(0,ChronoUnit.DAYS.between(due,date))); return new Result(days,daily.multiply(BigDecimal.valueOf(days)).setScale(2)); }
}
