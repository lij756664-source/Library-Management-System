package com.example.book.controller;

import com.example.book.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StatisticsDateInputTest {
    @Test
    void invalidDatesReturnInputErrorBeforeStatisticsRuns() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new StatisticsController((start, end) -> {
            calls.incrementAndGet();
            return Map.of();
        })).setControllerAdvice(new GlobalExceptionHandler()).build();

        // 分别覆盖起日、止日、双非法、非闰年2月29日和不存在的日期。
        for (String[] dates : new String[][] {
                {"bad", "bad"}, {"bad", "2026-11-03"}, {"2026-11-03", "bad"},
                {"2026-02-29", "2026-03-01"}, {"2026-11-03", "2026-11-31"}}) {
            mvc.perform(get("/api/statistics/overview").param("start", dates[0]).param("end", dates[1]))
                    .andExpect(status().isOk()) // 沿用项目的HTTP200 + 业务code约定
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("日期格式不正确，请使用年-月-日，例如2026-11-03"))
                    .andExpect(jsonPath("$.data").isEmpty());
        }
        assertEquals(0, calls.get());
    }

    @Test
    void validDatesAndAllHistoryStillReachStatistics() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new StatisticsController((start, end) -> {
            if (calls.getAndIncrement() == 0) {
                assertEquals(LocalDate.of(2024, 2, 29), start);
                assertEquals(LocalDate.of(2024, 3, 1), end);
            } else {
                assertEquals(null, start);
                assertEquals(null, end);
            }
            return Map.of("topBooks", java.util.List.of());
        })).setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/statistics/overview").param("start", "2024-02-29").param("end", "2024-03-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/statistics/overview"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertEquals(2, calls.get());
    }
}
