package com.example.book.controller;

import com.example.book.annotation.RequireRole;
import com.example.book.common.Result;
import com.example.book.entity.Reservation;
import com.example.book.service.IReservationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 预约相关接口：预约排队 / 取消 / 取书 / 记录查询
 * 前端 vue_front/src/api/reservation.js 与本类路径一一对应
 */
@RestController
@RequestMapping("/api/reservations")
@RequireRole({"ADMIN", "USER"})
public class ReservationController {

    private final IReservationService reservationService;

    public ReservationController(IReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /** 无库存预约：POST /api/reservations，加入 WAITING 队列 */
    @PostMapping
    public Result<Void> reserve(HttpServletRequest request, @RequestBody Map<String, Integer> body) {
        Integer bookId = body.get("bookId");
        if (bookId == null) {
            return Result.error(400, "图书ID不能为空");
        }
        Integer userId = (Integer) request.getAttribute("userId");
        reservationService.reserveBook(userId, bookId);
        return Result.success("预约成功，已加入排队", null);
    }

    /** 取消预约：DELETE /api/reservations/{id}；取消待取书会顺延下一位 */
    @DeleteMapping("/{id}")
    public Result<Void> cancel(HttpServletRequest request, @PathVariable Integer id) {
        Integer userId = (Integer) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        reservationService.cancel(userId, id, "ADMIN".equals(role));
        return Result.success("已取消预约", null);
    }

    /** 取书：PUT /api/reservations/{id}/pickup，READY → 正式借阅 */
    @PutMapping("/{id}/pickup")
    public Result<Void> pickup(HttpServletRequest request, @PathVariable Integer id) {
        Integer userId = (Integer) request.getAttribute("userId");
        reservationService.pickup(userId, id);
        return Result.success("取书成功，已转为借阅", null);
    }

    /** 我的预约：GET /api/reservations/my */
    @GetMapping("/my")
    public Result<List<Reservation>> myReservations(
            HttpServletRequest request,
            @RequestParam(required = false) String status) {
        Integer userId = (Integer) request.getAttribute("userId");
        return Result.success(reservationService.myReservations(userId, status));
    }

    /** 管理员预约列表：GET /api/reservations */
    @GetMapping
    @RequireRole("ADMIN")
    public Result<List<Reservation>> allReservations(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.success(reservationService.allReservations(keyword, status));
    }
}
