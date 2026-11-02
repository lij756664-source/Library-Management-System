package com.example.book.mapper;

import com.example.book.entity.SystemConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SystemConfigMapper {

    List<SystemConfig> findAll();
    List<SystemConfig> lockRules();
    int updateValue(@Param("key") String key,@Param("value") String value);

    SystemConfig findByKey(@Param("configKey") String configKey);
}
