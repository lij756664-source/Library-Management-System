package com.example.book.service;
import com.example.book.common.BusinessException;
import com.example.book.mapper.StatisticsMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service
public class StatisticsService {
 private final StatisticsMapper mapper;
 public StatisticsService(StatisticsMapper mapper) { this.mapper=mapper; }
 @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
 public Map<String,Object> query(LocalDate start,LocalDate end) {
  if((start==null)!=(end==null) || (start!=null && (start.isAfter(end)||end.equals(LocalDate.MAX)))) throw new BusinessException("起止日期须成对填写且开始日期不得晚于结束日期");
  LocalDateTime from=start==null?null:start.atStartOfDay(),to=end==null?null:end.plusDays(1).atStartOfDay();
  Map<String,Object> result=new HashMap<>();result.put("topBooks",mapper.top(from,to));result.put("categoryStats",mapper.categories(from,to));return result;
 }
}
