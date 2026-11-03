package com.example.book.service;

import com.example.book.controller.StatisticsController;
import com.example.book.entity.Book;
import com.example.book.entity.BorrowRecord;
import com.example.book.entity.Reservation;
import com.example.book.entity.SystemConfig;
import com.example.book.mapper.*;
import com.example.book.service.impl.*;
import com.example.book.vo.DashboardVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RuleSnapshotAndStatisticsTest {
    private final SystemConfigMapper configs = mock(SystemConfigMapper.class);
    private final BorrowMapper borrows = mock(BorrowMapper.class);
    private final BookMapper books = mock(BookMapper.class);
    private final IReservationService reservations = mock(IReservationService.class);
    private final IFineService fines = new FineServiceImpl();
    private final IRuleService rules = new RuleServiceImpl(configs);

    private void stubRules() {
        Map<String, String> values = Map.of("borrow_days", "20", "max_borrow_count", "3",
                "max_renew_count", "1", "renew_days", "5", "fine_per_day", "1.25");
        List<SystemConfig> rows = values.entrySet().stream().map(entry -> {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(entry.getKey());
            config.setConfigValue(entry.getValue());
            return config;
        }).toList();
        // 第二次读取会失败，确保业务没有在保存快照时重新读取规则。
        when(configs.findAll()).thenReturn(rows).thenThrow(new AssertionError("重复读取规则"));
        when(borrows.insert(any())).thenReturn(1);
    }

    private BorrowRecord assertSnapshot() {
        ArgumentCaptor<BorrowRecord> captor = ArgumentCaptor.forClass(BorrowRecord.class);
        verify(borrows).insert(captor.capture());
        BorrowRecord record = captor.getValue();
        assertEquals(record.getBorrowTime().plusDays(20), record.getDueTime());
        assertEquals(20, record.getBorrowDaysSnapshot());
        assertEquals(1, record.getMaxRenewCountSnapshot());
        assertEquals(5, record.getRenewDaysSnapshot());
        assertEquals(new BigDecimal("1.25"), record.getFinePerDaySnapshot());
        verify(configs).findAll();
        verifyNoMoreInteractions(configs);
        return record;
    }

    @Test void borrowUsesOneCompleteRuleSet() {
        stubRules();
        Book book = new Book();
        book.setStock(1);
        when(books.findById(2)).thenReturn(book);
        when(books.decreaseStock(2)).thenReturn(1);
        new BorrowServiceImpl(borrows, books, configs, reservations, rules, fines).borrowBook(1, 2);
        assertSnapshot();
    }

    @Test void pickupUsesOneCompleteRuleSet() {
        stubRules();
        ReservationMapper mapper = mock(ReservationMapper.class);
        Reservation ready = new Reservation();
        ready.setUserId(1);
        ready.setBookId(2);
        ready.setStatus("READY");
        when(mapper.findById(3)).thenReturn(ready);
        when(mapper.updateStatus(3, "FULFILLED")).thenReturn(1);
        new ReservationServiceImpl(mapper, books, borrows, configs, rules, fines).pickup(1, 3);
        assertSnapshot();
    }

    @Test void renewAndReturnUseHistoricalRulesWithoutReadingConfig() {
        BorrowRecord record = new BorrowRecord();
        record.setUserId(1);
        record.setBookId(2);
        record.setStatus("BORROWING");
        record.setDueTime(LocalDate.now(IFineService.ZONE).plusDays(2).atStartOfDay());
        record.setRenewCount(0);
        record.setMaxRenewCountSnapshot(2);
        record.setRenewDaysSnapshot(7);
        record.setFinePerDaySnapshot(new BigDecimal("0.50"));
        when(borrows.findById(3)).thenReturn(record);
        when(borrows.updateRenew(eq(3), any())).thenReturn(1);
        when(borrows.updateReturn(any())).thenReturn(1);
        BorrowServiceImpl service = new BorrowServiceImpl(borrows, books, configs, reservations, rules, fines);
        service.renewBook(1, 3);
        verify(borrows).updateRenew(3, record.getDueTime().plusDays(7));
        record.setDueTime(LocalDate.now(IFineService.ZONE).minusDays(2).atStartOfDay());
        service.returnBook(1, 3, false);
        ArgumentCaptor<BorrowRecord> captor = ArgumentCaptor.forClass(BorrowRecord.class);
        verify(borrows).updateReturn(captor.capture());
        assertEquals(new BigDecimal("1.00"), captor.getValue().getFineAmount());
        verifyNoInteractions(configs);
    }

    @Test void overviewKeepsGlobalDashboardAndFilteredCharts() {
        StatisticsMapper mapper = mock(StatisticsMapper.class);
        DashboardVO dashboard = new DashboardVO();
        when(mapper.getDashboardStats()).thenReturn(dashboard);
        BorrowRecord returned = new BorrowRecord();
        returned.setStatus("RETURNED");
        returned.setFineAmount(new BigDecimal("2.00"));
        BorrowRecord active = new BorrowRecord();
        active.setStatus("BORROWING");
        active.setDueTime(LocalDate.now(IFineService.ZONE).minusDays(2).atStartOfDay());
        active.setFinePerDaySnapshot(new BigDecimal("0.50"));
        when(borrows.findAllWithDetail(null, null)).thenReturn(List.of(returned, active));
        LocalDate from = LocalDate.of(2026, 10, 1), to = from.plusDays(2);
        var result = new StatisticsServiceImpl(mapper, borrows, fines).query(from, to);
        assertEquals(java.util.Set.of("dashboard", "categoryStats", "topBooks"), result.keySet());
        assertSame(dashboard, result.get("dashboard"));
        assertEquals(new BigDecimal("3.00"), dashboard.getTotalFine());
        verify(mapper).top(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        verify(mapper).categories(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        verify(mapper).getDashboardStats();
    }

    @Test void removedHttpRoutesAreAbsentAndOverviewRemains() throws Exception {
        IStatisticsService service = mock(IStatisticsService.class);
        when(service.query(null, null)).thenReturn(Map.of());
        var mvc = MockMvcBuilders.standaloneSetup(new StatisticsController(service)).build();
        for (String route : List.of("dashboard", "category", "top-books")) {
            mvc.perform(get("/api/statistics/" + route)).andExpect(status().isNotFound());
        }
        verifyNoInteractions(service);
        mvc.perform(get("/api/statistics/overview")).andExpect(status().isOk());
        verify(service).query(null, null);
    }
}
