package com.example.book.service;

import com.example.book.entity.Reservation;

import java.util.List;

public interface IReservationService {

    void reserveBook(Integer userId, Integer bookId);

    void cancel(Integer userId, Integer reservationId, boolean isAdmin);

    void pickup(Integer userId, Integer reservationId);

    /** 图书归还后：有排队则通知队首取书，否则回补可借库存 */
    void assignReturnedCopy(Integer bookId);

    List<Reservation> myReservations(Integer userId, String status);

    List<Reservation> allReservations(String keyword, String status);
}
