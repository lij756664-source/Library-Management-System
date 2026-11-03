package com.example.book.mapper;

import com.example.book.entity.BorrowRecord;
import com.example.book.vo.BookStatVO;
import com.example.book.vo.CategoryStatVO;
import com.example.book.vo.DashboardVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BorrowMapper {

    /** 新增借阅记录 */
    int insert(BorrowRecord record);

    BorrowRecord findById(@Param("id") Integer id);

    /** 当前在借册数 */
    int countActiveByUserId(@Param("userId") Integer userId);

    /** 是否已在借同一本书 */
    int countActiveByUserAndBook(@Param("userId") Integer userId, @Param("bookId") Integer bookId);

    int countByBookId(@Param("bookId") Integer bookId);

    /** 续借：更新应还时间 */
    int updateRenew(@Param("id") Integer id, @Param("dueTime") java.time.LocalDateTime dueTime);

    /** 归还：改为 RETURNED */
    int updateReturn(BorrowRecord record);

    /** 读者借阅记录 */
    List<BorrowRecord> findByUserId(@Param("userId") Integer userId,
                                   @Param("status") String status,
                                   @Param("keyword") String keyword);

    /** 管理员借阅记录 */
    List<BorrowRecord> findAllWithDetail(@Param("keyword") String keyword, @Param("status") String status);

    DashboardVO getDashboardStats();

    List<CategoryStatVO> statByCategory();

    List<BookStatVO> statTopBooks(@Param("limit") int limit);
}
