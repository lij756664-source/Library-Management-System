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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationServiceImpl implements IReservationService {

    private final ReservationMapper reservationMapper;
    private final BookMapper bookMapper;
    private final BorrowMapper borrowMapper;
    private final SystemConfigMapper systemConfigMapper;

    public ReservationServiceImpl(ReservationMapper reservationMapper, BookMapper bookMapper,
                                  BorrowMapper borrowMapper, SystemConfigMapper systemConfigMapper) {
        this.reservationMapper = reservationMapper;
        this.bookMapper = bookMapper;
        this.borrowMapper = borrowMapper;
        this.systemConfigMapper = systemConfigMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reserveBook(Integer userId, Integer bookId) {
        processExpiredReady();

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

        LocalDateTime now = LocalDateTime.now();
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
        if (reservation.getExpireTime() != null && LocalDateTime.now().isAfter(reservation.getExpireTime())) {
            expireAndPromote(reservation);
            throw new BusinessException(400, "取书期限已过，预约已失效");
        }

        int maxBorrow = getIntConfig("max_borrow_count", 5);
        if (borrowMapper.countActiveByUserId(userId) >= maxBorrow) {
            throw new BusinessException(400, "已达最大借阅数量（" + maxBorrow + "册），请先归还后再取书");
        }
        if (borrowMapper.countActiveByUserAndBook(userId, reservation.getBookId()) > 0) {
            throw new BusinessException(400, "您已借阅该图书");
        }

        int borrowDays = getIntConfig("borrow_days", 30);
        LocalDateTime now = LocalDateTime.now();
        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setBookId(reservation.getBookId());
        record.setBorrowTime(now);
        record.setDueTime(now.plusDays(borrowDays));
        record.setStatus("BORROWING");
        record.setRenewCount(0);
        if (borrowMapper.insert(record) <= 0) {
            throw new BusinessException("创建借阅记录失败");
        }
        if (reservationMapper.updateStatus(reservationId, "FULFILLED") <= 0) {
            throw new BusinessException("预约状态更新失败");
        }
    }

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

    private void processExpiredReady() {
        List<Reservation> expired = reservationMapper.findExpiredReady(LocalDateTime.now());
        for (Reservation reservation : expired) {
            expireAndPromote(reservation);
        }
    }

    private void expireAndPromote(Reservation reservation) {
        if (!"READY".equals(reservation.getStatus())) {
            return;
        }
        reservationMapper.updateStatus(reservation.getId(), "EXPIRED");
        promoteOrRestock(reservation.getBookId());
    }

    /** 队首进入待取书；无人排队则回补可借库存 */
    private void promoteOrRestock(Integer bookId) {
        Reservation waiting = reservationMapper.findFirstWaitingByBook(bookId);
        if (waiting == null) {
            bookMapper.increaseStock(bookId);
            return;
        }
        int pickupDays = getIntConfig("pickup_days", 3);
        LocalDateTime expireTime = LocalDateTime.now().plusDays(pickupDays);
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
