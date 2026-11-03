package com.example.book.service.impl;

import com.example.book.common.BusinessException;
import com.example.book.entity.Book;
import com.example.book.entity.BorrowRecord;
import com.example.book.entity.Reservation;
import com.example.book.entity.SystemConfig;
import com.example.book.mapper.BookMapper;
import com.example.book.mapper.BorrowMapper;
import com.example.book.mapper.ReservationMapper;
import com.example.book.mapper.SystemConfigMapper;
import com.example.book.service.IReservationService;
import com.example.book.service.IRuleService;
import com.example.book.service.IFineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 预约排队：无库存预约、取消顺延、待取书取书、归还后分配副本 */
@Service
public class ReservationServiceImpl implements IReservationService {

    private final ReservationMapper reservationMapper;
    private final BookMapper bookMapper;
    private final IRuleService ruleService;
    private final IFineService fineService;
    private final BorrowMapper borrowMapper;
    private final SystemConfigMapper systemConfigMapper;

    public ReservationServiceImpl(ReservationMapper reservationMapper, BookMapper bookMapper,
                                  BorrowMapper borrowMapper, SystemConfigMapper systemConfigMapper,
                                  IRuleService ruleService, IFineService fineService) {
        this.reservationMapper = reservationMapper;
        this.bookMapper = bookMapper;
        this.ruleService = ruleService;
        this.fineService = fineService;
        this.borrowMapper = borrowMapper;
        this.systemConfigMapper = systemConfigMapper;
    }

    /**
     * 无库存预约：必须没库存、未在借这本书、未重复预约，且未超预约上限。
     * 成功后状态为 WAITING，按预约时间排队。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reserveBook(Integer userId, Integer bookId) {
        processExpiredReady(); // 先清理过期未取，避免队列脏数据

        Book book = bookMapper.findById(bookId);
        if (book == null) {
            throw new BusinessException(404, "图书不存在");
        }
        if (book.getStock() != null && book.getStock() > 0) {
            throw new BusinessException(400, "该书尚有库存，请直接借阅");
        }
        if (borrowMapper.countActiveByUserAndBook(userId, bookId) > 0) {
            throw new BusinessException(400, "您已借阅该图书，无需预约");
        }
        if (reservationMapper.findActiveByUserAndBook(userId, bookId) != null) {
            throw new BusinessException(400, "您已预约该图书，请勿重复预约");
        }

        int maxReserve = getIntConfig("max_reservation_count", 3);
        if (reservationMapper.countActiveByUserId(userId) >= maxReserve) {
            throw new BusinessException(400, "已达最大预约数量（" + maxReserve + "本）");
        }

        LocalDateTime now = LocalDateTime.now(IFineService.ZONE);
        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setBookId(bookId);
        reservation.setStatus("WAITING");
        reservation.setCreateTime(now);
        reservation.setUpdateTime(now);
        if (reservationMapper.insert(reservation) <= 0) {
            throw new BusinessException("预约失败");
        }
    }

    /**
     * 取消预约：读者只能取消自己的 WAITING/READY。
     * 若取消的是待取书（READY），该书要交给下一位或回补库存。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Integer userId, Integer reservationId, boolean isAdmin) {
        processExpiredReady();

        Reservation reservation = reservationMapper.findById(reservationId);
        if (reservation == null) {
            throw new BusinessException(404, "预约记录不存在");
        }
        if (!isAdmin && !reservation.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能取消自己的预约");
        }
        if (!"WAITING".equals(reservation.getStatus()) && !"READY".equals(reservation.getStatus())) {
            throw new BusinessException(400, "该预约已结束，无法取消");
        }

        boolean wasReady = "READY".equals(reservation.getStatus());
        if (reservationMapper.updateStatus(reservationId, "CANCELLED") <= 0) {
            throw new BusinessException("取消预约失败");
        }
        if (wasReady) {
            promoteOrRestock(reservation.getBookId());
        }
    }

    /**
     * 待取书领取：必须是本人且状态 READY、未过取书期限。
     * 成功后插入借阅记录，预约改为 FULFILLED（书在进入 READY 时已预留，此处不再减库存）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pickup(Integer userId, Integer reservationId) {
        processExpiredReady();

        Reservation reservation = reservationMapper.findById(reservationId);
        if (reservation == null) {
            throw new BusinessException(404, "预约记录不存在");
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能领取自己预约的图书");
        }
        if (!"READY".equals(reservation.getStatus())) {
            throw new BusinessException(400, "尚未轮到您取书，请继续排队");
        }
        if (reservation.getExpireTime() != null && LocalDateTime.now(IFineService.ZONE).isAfter(reservation.getExpireTime())) {
            expireAndPromote(reservation);
            throw new BusinessException(400, "取书期限已过，预约已失效");
        }

        // 一次读取完整规则，本次校验、到期时间和快照使用同一版本。
        Map<String, String> rules = ruleService.current();
        int maxBorrow = Integer.parseInt(rules.get("max_borrow_count"));
        if (borrowMapper.countActiveByUserId(userId) >= maxBorrow) {
            throw new BusinessException(400, "已达最大借阅数量（" + maxBorrow + "册），请先归还后再取书");
        }
        if (borrowMapper.countActiveByUserAndBook(userId, reservation.getBookId()) > 0) {
            throw new BusinessException(400, "您已借阅该图书");
        }

        int borrowDays = Integer.parseInt(rules.get("borrow_days"));
        LocalDateTime now = LocalDateTime.now(IFineService.ZONE);
        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setBookId(reservation.getBookId());
        record.setBorrowTime(now);
        record.setDueTime(now.plusDays(borrowDays));
        record.setStatus("BORROWING");
        record.setRenewCount(0);
        ruleService.snapshot(record, rules);
        if (borrowMapper.insert(record) <= 0) {
            throw new BusinessException("创建借阅记录失败");
        }
        if (reservationMapper.updateStatus(reservationId, "FULFILLED") <= 0) {
            throw new BusinessException("预约状态更新失败");
        }
    }

    /** 有人还书后调用：有排队则队首变 READY，否则库存 +1 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignReturnedCopy(Integer bookId) {
        processExpiredReady();
        promoteOrRestock(bookId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Reservation> myReservations(Integer userId, String status) {
        processExpiredReady();
        return reservationMapper.findByUserId(userId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Reservation> allReservations(String keyword, String status) {
        processExpiredReady();
        return reservationMapper.findAllWithDetail(keyword, status);
    }

    /** 把超过取书期限仍未取的 READY 作废，并顺延下一位 */
    private void processExpiredReady() {
        List<Reservation> expired = reservationMapper.findExpiredReady(LocalDateTime.now(IFineService.ZONE));
        for (Reservation reservation : expired) {
            expireAndPromote(reservation);
        }
    }

    /** 单条待取书过期：EXPIRED，再把书分给下一位 */
    private void expireAndPromote(Reservation reservation) {
        if (!"READY".equals(reservation.getStatus())) {
            return;
        }
        reservationMapper.updateStatus(reservation.getId(), "EXPIRED");
        promoteOrRestock(reservation.getBookId());
    }

    /**
     * 队列核心：找该书最早的 WAITING。
     * 有人 → 改为 READY 并设置取书截止时间；没人 → 库存加回可借。
     */
    private void promoteOrRestock(Integer bookId) {
        Reservation waiting = reservationMapper.findFirstWaitingByBook(bookId);
        if (waiting == null) {
            bookMapper.increaseStock(bookId);
            return;
        }
        int pickupDays = getIntConfig("pickup_days", 3); // 默认 3 天内取书
        LocalDateTime expireTime = LocalDateTime.now(IFineService.ZONE).plusDays(pickupDays);
        if (reservationMapper.updateToReady(waiting.getId(), expireTime) <= 0) {
            bookMapper.increaseStock(bookId);
        }
    }

    private int getIntConfig(String key, int defaultValue) {
        SystemConfig config = systemConfigMapper.findByKey(key);
        if (config == null) {
            return defaultValue;
        }
        return Integer.parseInt(config.getConfigValue());
    }
}
