package com.example.book.mapper;

import com.example.book.entity.Reservation;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationMapper {

    /** 新增预约 */
    int insert(Reservation reservation);

    Reservation findById(@Param("id") Integer id);

    /** 该用户对该书是否已有有效预约 */
    Reservation findActiveByUserAndBook(@Param("userId") Integer userId, @Param("bookId") Integer bookId);

    /** 有效预约本数（上限校验） */
    int countActiveByUserId(@Param("userId") Integer userId);

    int countActiveByBookId(@Param("bookId") Integer bookId);

    /** 队首 WAITING，用于顺延 */
    Reservation findFirstWaitingByBook(@Param("bookId") Integer bookId);

    /** 已过取书期限的 READY */
    List<Reservation> findExpiredReady(@Param("now") LocalDateTime now);

    /** WAITING → READY */
    int updateToReady(@Param("id") Integer id, @Param("expireTime") LocalDateTime expireTime);

    int updateStatus(@Param("id") Integer id, @Param("status") String status);

    List<Reservation> findByUserId(@Param("userId") Integer userId, @Param("status") String status);

    List<Reservation> findAllWithDetail(@Param("keyword") String keyword, @Param("status") String status);
}
