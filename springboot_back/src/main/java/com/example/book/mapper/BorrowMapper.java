package com.example.book.mapper;

import com.example.book.entity.BorrowRecord;
import com.example.book.vo.BookStatVO;
import com.example.book.vo.CategoryStatVO;
import com.example.book.vo.DashboardVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BorrowMapper {

    int insert(BorrowRecord record);

    BorrowRecord findById(@Param("id") Integer id);

    int countActiveByUserId(@Param("userId") Integer userId);

    int countActiveByUserAndBook(@Param("userId") Integer userId, @Param("bookId") Integer bookId);

    int countByBookId(@Param("bookId") Integer bookId);

    int updateRenew(@Param("id") Integer id, @Param("dueTime") java.time.LocalDateTime dueTime);

    int updateReturn(BorrowRecord record);

    List<BorrowRecord> findByUserId(@Param("userId") Integer userId, @Param("status") String status);

    List<BorrowRecord> findAllWithDetail(@Param("keyword") String keyword, @Param("status") String status);

    DashboardVO getDashboardStats();

    List<CategoryStatVO> statByCategory();

    List<BookStatVO> statTopBooks(@Param("limit") int limit);
}
