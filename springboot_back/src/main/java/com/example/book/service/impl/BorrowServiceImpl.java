package com.example.book.service.impl;

import com.example.book.common.BusinessException;
import com.example.book.entity.Book;
import com.example.book.entity.BorrowRecord;
import com.example.book.entity.SystemConfig;
import com.example.book.mapper.BookMapper;
import com.example.book.mapper.BorrowMapper;
import com.example.book.mapper.SystemConfigMapper;
import com.example.book.service.IBorrowService;
import com.example.book.service.IReservationService;
import com.example.book.service.IRuleService;
import com.example.book.service.IFineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BorrowServiceImpl implements IBorrowService {

    private final IRuleService ruleService;
    private final IFineService fineService;
    private final BorrowMapper borrowMapper;
    private final BookMapper bookMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final IReservationService reservationService;

    public BorrowServiceImpl(BorrowMapper borrowMapper, BookMapper bookMapper,
                             SystemConfigMapper systemConfigMapper,
                             IReservationService reservationService,
                             IRuleService ruleService, IFineService fineService) {
        this.ruleService = ruleService;
        this.fineService = fineService;
        this.borrowMapper = borrowMapper;
        this.bookMapper = bookMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.reservationService = reservationService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void borrowBook(Integer userId, Integer bookId) {
        Book book = bookMapper.findById(bookId);
        if (book == null) {
            throw new BusinessException(404, "图书不存在");
        }
        if (book.getStock() == null || book.getStock() <= 0) {
            throw new BusinessException(400, "该书暂无库存，无法借阅");
        }

        // 一次读取完整规则，本次校验、到期时间和快照使用同一版本。
        Map<String, String> rules = ruleService.current();
        int maxBorrow = Integer.parseInt(rules.get("max_borrow_count"));
        if (borrowMapper.countActiveByUserId(userId) >= maxBorrow) {
            throw new BusinessException(400, "已达最大借阅数量（" + maxBorrow + "册）");
        }

        if (borrowMapper.countActiveByUserAndBook(userId, bookId) > 0) {
            throw new BusinessException(400, "您已借阅该图书，不能重复借阅");
        }

        int borrowDays = Integer.parseInt(rules.get("borrow_days"));
        LocalDateTime now = LocalDateTime.now(IFineService.ZONE);

        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setBookId(bookId);
        record.setBorrowTime(now);
        record.setDueTime(now.plusDays(borrowDays));
        record.setStatus("BORROWING");
        record.setRenewCount(0);
        ruleService.snapshot(record, rules);

        if (borrowMapper.insert(record) <= 0) {
            throw new BusinessException("创建借阅记录失败");
        }
        if (bookMapper.decreaseStock(bookId) <= 0) {
            throw new BusinessException(400, "库存扣减失败，请重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renewBook(Integer userId, Integer borrowId) {
        BorrowRecord record = borrowMapper.findById(borrowId);
        if (record == null) {
            throw new BusinessException(404, "借阅记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能续借自己的图书");
        }
        if (!"BORROWING".equals(record.getStatus())) {
            throw new BusinessException(400, "该记录已归还，无法续借");
        }
        if (Boolean.TRUE.equals(record.getOverdue())) {
            throw new BusinessException(400, "图书已逾期，请先归还后再借阅");
        }

        // 续借沿用借阅时的规则，不读取当前配置。
        int maxRenew = record.getMaxRenewCountSnapshot();
        if (record.getRenewCount() >= maxRenew) {
            throw new BusinessException(400, "已达最大续借次数（" + maxRenew + "次）");
        }

        int renewDays = record.getRenewDaysSnapshot();
        LocalDateTime newDueTime = record.getDueTime().plusDays(renewDays);
        if (borrowMapper.updateRenew(borrowId, newDueTime) <= 0) {
            throw new BusinessException("续借失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnBook(Integer userId, Integer borrowId, boolean isAdmin) {
        BorrowRecord record = borrowMapper.findById(borrowId);
        if (record == null) {
            throw new BusinessException(404, "借阅记录不存在");
        }
        if (!isAdmin && !record.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能归还自己的图书");
        }
        if (!"BORROWING".equals(record.getStatus())) {
            throw new BusinessException(400, "该图书已归还");
        }

        LocalDateTime now = LocalDateTime.now(IFineService.ZONE);
        // 归还按原日费结算，管理员修改当前规则不影响本记录。
        var calculation = fineService.calculate(record.getDueTime().toLocalDate(),
                record.getFinePerDaySnapshot(), now.toLocalDate());
        int overdueDays = calculation.overdueDays();
        BigDecimal fineAmount = calculation.amount();

        BorrowRecord update = new BorrowRecord();
        update.setId(borrowId);
        update.setReturnTime(now);
        update.setOverdueDays(overdueDays);
        update.setFineAmount(fineAmount);

        if (borrowMapper.updateReturn(update) <= 0) {
            throw new BusinessException("归还失败");
        }
        reservationService.assignReturnedCopy(record.getBookId());
    }

    @Override
    public List<BorrowRecord> myBorrows(Integer userId, String status, String keyword) {
        List<BorrowRecord> records = borrowMapper.findByUserId(userId, status, keyword);
        records.forEach(this::fillEstimatedFine);
        return records;
    }

    @Override
    public List<BorrowRecord> allBorrows(String keyword, String status) {
        List<BorrowRecord> records = borrowMapper.findAllWithDetail(keyword, status);
        records.forEach(this::fillEstimatedFine);
        return records;
    }

    /** 借阅中且已逾期时，计算预估罚款（归还前展示） */
    private void fillEstimatedFine(BorrowRecord record) {
        if (!"BORROWING".equals(record.getStatus()) || record.getDueTime() == null) {
            return;
        }
        var today = java.time.LocalDate.now(IFineService.ZONE);
        var result = fineService.calculate(record.getDueTime().toLocalDate(),
                record.getFinePerDaySnapshot(), today);
        record.setOverdueDays(result.overdueDays());
        record.setFineAmount(result.amount());
        record.setOverdue(result.overdueDays() > 0);
        record.setRemainDays(ChronoUnit.DAYS.between(today, record.getDueTime().toLocalDate()));
    }

    @Override
    public Map<String, String> getBorrowRules() {
        Map<String, String> rules = new HashMap<>();
        for (SystemConfig config : systemConfigMapper.findAll()) {
            rules.put(config.getConfigKey(), config.getConfigValue());
        }
        return rules;
    }

}
