package com.example.book.mapper;

import com.example.book.vo.BookStatVO;
import com.example.book.vo.CategoryStatVO;
import com.example.book.vo.DashboardVO;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface StatisticsMapper {
    DashboardVO getDashboardStats();

    List<BookStatVO> top(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<CategoryStatVO> categories(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
