package com.example.book.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class DateBoundaryTest {
    private final FineService service = new FineService();

    @Test void beijingMidnightAndReturnDates() {
        var due = LocalDate.of(2026, 10, 10);
        var daily = new BigDecimal("0.50");
        var before = Instant.parse("2026-10-10T15:59:00Z").atZone(FineService.ZONE).toLocalDate();
        var after = Instant.parse("2026-10-10T16:01:00Z").atZone(FineService.ZONE).toLocalDate();
        assertEquals(new BigDecimal("0.00"), service.calculate(due, daily, before).amount());
        assertEquals(new BigDecimal("0.50"), service.calculate(due, daily, after).amount());
        assertEquals(new BigDecimal("1.00"), service.calculate(due, daily, due.plusDays(2)).amount());
        assertEquals(new BigDecimal("1.50"), service.calculate(due, daily, due.plusDays(3)).amount());
        // Pure calculation accepts an explicit date; stored final amounts are checked through API separately.
        assertEquals(2, service.calculate(due, BigDecimal.ZERO, due.plusDays(2)).overdueDays());
        assertEquals(new BigDecimal("0.00"), service.calculate(due, BigDecimal.ZERO, due.plusDays(2)).amount());
    }

}
