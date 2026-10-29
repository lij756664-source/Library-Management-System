package com.example.book.mapper;

import com.example.book.entity.Reservation;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationMapper {

    int insert(Reservation reservation);

    Reservation findById(@Param("id") Integer id);

    Reservation findActiveByUserAndBook(@Param("userId") Integer userId, @Param("bookId") Integer bookId);

    int countActiveByUserId(@Param("userId") Integer userId);

    int countActiveByBookId(@Param("bookId") Integer bookId);

    Reservation findFirstWaitingByBook(@Param("bookId") Integer bookId);

    List<Reservation> findExpiredReady(@Param("now") LocalDateTime now);

    int updateToReady(@Param("id") Integer id, @Param("expireTime") LocalDateTime expireTime);

    int updateStatus(@Param("id") Integer id, @Param("status") String status);

    List<Reservation> findByUserId(@Param("userId") Integer userId, @Param("status") String status);

    List<Reservation> findAllWithDetail(@Param("keyword") String keyword, @Param("status") String status);
}
