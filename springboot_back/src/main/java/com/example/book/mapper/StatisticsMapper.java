package com.example.book.mapper;
import com.example.book.vo.*;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;
public interface StatisticsMapper {
 List<BookStatVO> top(@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);
 List<CategoryStatVO> categories(@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);
}
