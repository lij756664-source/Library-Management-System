package com.example.book.service;

import com.example.book.service.impl.StatisticsServiceImpl;

import com.example.book.service.impl.FineServiceImpl;

import com.example.book.service.impl.RuleServiceImpl;
import com.example.book.mapper.SystemConfigMapper;
import com.example.book.entity.SystemConfig;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RuleFineTest {
 @Test void invalidStatisticsRangeDoesNotReadData() {
  var mapper=mock(com.example.book.mapper.StatisticsMapper.class);
  var service=new StatisticsServiceImpl(mapper, mock(com.example.book.mapper.BorrowMapper.class), new FineServiceImpl());var date=LocalDate.of(2026,10,10);
  assertThrows(RuntimeException.class,()->service.query(date,null));
  assertThrows(RuntimeException.class,()->service.query(date,date.minusDays(1)));verifyNoInteractions(mapper);
 }
 @Test void mybatisMappingsParse() throws Exception {
  var configuration=new org.apache.ibatis.session.Configuration();
  for(String name:List.of("BorrowMapper","SystemConfigMapper","StatisticsMapper")) {
   String resource="mapper/"+name+".xml";
   try(var input=getClass().getClassLoader().getResourceAsStream(resource)) {
    assertNotNull(input);new org.apache.ibatis.builder.xml.XMLMapperBuilder(input,configuration,resource,configuration.getSqlFragments()).parse();
   }
  }
 }

 @Test void naturalDaysAndZeroRate() {
  FineServiceImpl service=new FineServiceImpl();LocalDate due=LocalDate.of(2026,10,10);
  assertEquals(new BigDecimal("0.00"),service.calculate(due,new BigDecimal("0.50"),due).amount());
  assertEquals(new BigDecimal("1.50"),service.calculate(due,new BigDecimal("0.50"),due.plusDays(3)).amount());
  assertEquals(2,service.calculate(due,BigDecimal.ZERO,due.plusDays(2)).overdueDays());
  assertEquals(0,service.calculate(due,BigDecimal.ONE,due.minusDays(1)).overdueDays());
 }
 @Test void rejectsPrecisionAndFractionalIntegersBeforeWriting() {
  SystemConfigMapper mapper=mock(SystemConfigMapper.class);RuleServiceImpl service=new RuleServiceImpl(mapper);
  Map<String,String> values=new HashMap<>(Map.of("borrow_days","30","max_renew_count","2","renew_days","7","fine_per_day","0.501","max_borrow_count","5"));
  assertThrows(RuntimeException.class,()->service.save(values));verifyNoInteractions(mapper);
  values.put("fine_per_day","0.50");values.put("renew_days","1.5");assertThrows(RuntimeException.class,()->service.save(values));verifyNoInteractions(mapper);
 }
 @Test void savesZeroRateAndDisabledRenewal() {
  SystemConfigMapper mapper=mock(SystemConfigMapper.class);List<SystemConfig> rows=new ArrayList<>();
  Map<String,String> values=Map.of("borrow_days","30","max_renew_count","0","renew_days","7","fine_per_day","0.00","max_borrow_count","5");
  values.forEach((k,v)->{SystemConfig c=new SystemConfig();c.setConfigKey(k);c.setConfigValue(v);rows.add(c);});
  when(mapper.lockRules()).thenReturn(rows);when(mapper.findAll()).thenReturn(rows);when(mapper.updateValue(anyString(),anyString())).thenReturn(1);
  assertEquals("0.00",new RuleServiceImpl(mapper).save(values).get("fine_per_day"));
  var order = inOrder(mapper);
  order.verify(mapper).lockRules();
  order.verify(mapper, times(5)).updateValue(anyString(), anyString());
  order.verify(mapper).findAll();
 }
}
