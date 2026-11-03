package com.example.book.service;

import com.example.book.entity.Reservation;

import java.util.List;

public interface IReservationService {

    /** 无库存图书预约排队 */
    void reserveBook(Integer userId, Integer bookId);

    /** 取消预约；取消 READY 时触发队列顺延 */
    void cancel(Integer userId, Integer reservationId, boolean isAdmin);

    /** 待取书领取并转为借阅 */
    void pickup(Integer userId, Integer reservationId);

    /** 图书归还后：有排队则通知队首取书，否则回补可借库存 */
    void assignReturnedCopy(Integer bookId);

    List<Reservation> myReservations(Integer userId, String status);

    List<Reservation> allReservations(String keyword, String status);
}
