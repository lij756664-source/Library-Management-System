package com.example.book.service.impl;

import com.example.book.common.BusinessException;
import com.example.book.entity.BorrowRecord;
import com.example.book.mapper.BorrowMapper;
import com.example.book.mapper.StatisticsMapper;
import com.example.book.service.IFineService;
import com.example.book.service.IStatisticsService;
import com.example.book.vo.DashboardVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class StatisticsServiceImpl implements IStatisticsService {
    private final StatisticsMapper mapper;
    private final BorrowMapper borrowMapper;
    private final IFineService fineService;

    public StatisticsServiceImpl(StatisticsMapper mapper, BorrowMapper borrowMapper,
                                 IFineService fineService) {
        this.mapper = mapper;
        this.borrowMapper = borrowMapper;
        this.fineService = fineService;
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Map<String, Object> query(LocalDate start, LocalDate end) {
        if ((start == null) != (end == null)
                || (start != null && (start.isAfter(end) || end.equals(LocalDate.MAX)))) {
            throw new BusinessException("起止日期须成对填写且开始日期不得晚于结束日期");
        }
        // 使用左闭右开区间，包含结束日期当天的全部借阅记录。
        LocalDateTime from = start == null ? null : start.atStartOfDay();
        LocalDateTime to = end == null ? null : end.plusDays(1).atStartOfDay();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("topBooks", mapper.top(from, to));
        result.put("categoryStats", mapper.categories(from, to));
        // 概况显示全局数据，不随借出日期筛选变化。
        DashboardVO dashboard = mapper.getDashboardStats();
        dashboard.setTotalFine(totalFine(LocalDate.now(IFineService.ZONE)));
        result.put("dashboard", dashboard);
        return result;
    }

    /** 合计包含已归还最终罚金和未归还预估罚金，不代表已收款。 */
    private BigDecimal totalFine(LocalDate today) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BorrowRecord record : borrowMapper.findAllWithDetail(null, null)) {
            if ("RETURNED".equals(record.getStatus()) && record.getFineAmount() != null) {
                sum = sum.add(record.getFineAmount());
            } else if ("BORROWING".equals(record.getStatus()) && record.getDueTime() != null) {
                sum = sum.add(fineService.calculate(record.getDueTime().toLocalDate(),
                        record.getFinePerDaySnapshot(), today).amount());
            }
        }
        return sum;
    }
}
