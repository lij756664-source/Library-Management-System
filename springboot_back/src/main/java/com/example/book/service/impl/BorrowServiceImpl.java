package com.example.book.service.impl;

import com.example.book.common.BusinessException;
import com.example.book.entity.Book;
import com.example.book.entity.BorrowRecord;
import com.example.book.entity.SystemConfig;
import com.example.book.mapper.BookMapper;
import com.example.book.mapper.BorrowMapper;
import com.example.book.mapper.SystemConfigMapper;
import com.example.book.service.IBorrowService;
import com.example.book.service.IFineService;
import com.example.book.service.IReservationService;
import com.example.book.service.IRuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 图书流通：借阅、续借、归还、借阅记录查询 */
@Service
public class BorrowServiceImpl implements IBorrowService {

    private final BorrowMapper borrowMapper;
    private final BookMapper bookMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final IReservationService reservationService;
    private final IRuleService ruleService;
    private final IFineService fineService;

    public BorrowServiceImpl(BorrowMapper borrowMapper, BookMapper bookMapper,
                             SystemConfigMapper systemConfigMapper,
                             IReservationService reservationService,
                             IRuleService ruleService, IFineService fineService) {
        this.borrowMapper = borrowMapper;
        this.bookMapper = bookMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.reservationService = reservationService;
        this.ruleService = ruleService;
        this.fineService = fineService;
    }

    /**
     * 图书借阅：校验通过后写借阅记录并扣库存。
     * 事务保证「记了一笔借阅」和「库存减 1」同时成功或同时回滚。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void borrowBook(Integer userId, Integer bookId) {
        Book book = bookMapper.findById(bookId);
        if (book == null) {
            throw new BusinessException(404, "图书不存在");
        }
        // 无库存不能借，前端此时应走预约
        if (book.getStock() == null || book.getStock() <= 0) {
            throw new BusinessException(400, "该书暂无库存，无法借阅");
        }

        // 一次读取完整规则，借期、上限和快照用同一版本
        Map<String, String> rules = ruleService.current();
        int maxBorrow = Integer.parseInt(rules.get("max_borrow_count"));
        if (borrowMapper.countActiveByUserId(userId) >= maxBorrow) {
            throw new BusinessException(400, "已达最大借阅数量（" + maxBorrow + "册）");
        }

        // 同一本书未还清前不能再借
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
        // WHERE stock > 0，并发下最后一本只会扣成功一次
        if (bookMapper.decreaseStock(bookId) <= 0) {
            throw new BusinessException(400, "库存扣减失败，请重试");
        }
    }

    /**
     * 图书续借：只能续自己的、未归还且未逾期的记录。
     * 应还时间在原到期日上延长 renew_days（默认 14 天）。
     */
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
        LocalDate today = LocalDate.now(IFineService.ZONE);
        if (record.getDueTime() != null && today.isAfter(record.getDueTime().toLocalDate())) {
            throw new BusinessException(400, "图书已逾期，请先归还后再借阅");
        }

        // 续借次数和延长天数用借阅时快照，不再读当前配置
        int maxRenew = record.getMaxRenewCountSnapshot() != null ? record.getMaxRenewCountSnapshot() : 2;
        if (record.getRenewCount() >= maxRenew) {
            throw new BusinessException(400, "已达最大续借次数（" + maxRenew + "次）");
        }

        int renewDays = record.getRenewDaysSnapshot() != null ? record.getRenewDaysSnapshot() : 14;
        LocalDateTime newDueTime = record.getDueTime().plusDays(renewDays);
        if (borrowMapper.updateRenew(borrowId, newDueTime) <= 0) {
            throw new BusinessException("续借失败");
        }
    }

    /**
     * 图书归还：读者只能还自己的，管理员可代还。
     * 逾期按天计罚并写入记录；还书后把副本交给预约队列（有人排队则队首待取，无人则库存+1）。
     */
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
        BigDecimal daily = record.getFinePerDaySnapshot() != null
                ? record.getFinePerDaySnapshot() : getDecimalConfig("fine_per_day", new BigDecimal("0.5"));
        IFineService.Result fine = fineService.calculate(record.getDueTime().toLocalDate(), daily, now.toLocalDate());

        BorrowRecord update = new BorrowRecord();
        update.setId(borrowId);
        update.setReturnTime(now);
        update.setOverdueDays(fine.overdueDays());
        update.setFineAmount(fine.amount());

        if (borrowMapper.updateReturn(update) <= 0) {
            throw new BusinessException("归还失败");
        }
        // 流通衔接：归还不直接加库存，优先分给预约队首
        reservationService.assignReturnedCopy(record.getBookId());
    }

    /** 读者借阅记录查询，逾期未还的补上预估罚款便于展示 */
    @Override
    public List<BorrowRecord> myBorrows(Integer userId, String status, String keyword) {
        List<BorrowRecord> records = borrowMapper.findByUserId(userId, status, keyword);
        records.forEach(this::fillEstimatedFine);
        return records;
    }

    /** 管理员全馆借阅记录查询 */
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
        LocalDate today = LocalDate.now(IFineService.ZONE);
        LocalDate due = record.getDueTime().toLocalDate();
        if (!today.isAfter(due)) {
            return;
        }
        BigDecimal daily = record.getFinePerDaySnapshot() != null
                ? record.getFinePerDaySnapshot() : getDecimalConfig("fine_per_day", new BigDecimal("0.5"));
        IFineService.Result fine = fineService.calculate(due, daily, today);
        record.setOverdueDays(fine.overdueDays());
        record.setFineAmount(fine.amount());
        record.setOverdue(true);
        record.setRemainDays((long) -fine.overdueDays());
    }

    @Override
    public Map<String, String> getBorrowRules() {
        return ruleService.current();
    }

    private BigDecimal getDecimalConfig(String key, BigDecimal defaultValue) {
        SystemConfig config = systemConfigMapper.findByKey(key);
        if (config == null) {
            return defaultValue;
        }
        return new BigDecimal(config.getConfigValue());
    }
}
