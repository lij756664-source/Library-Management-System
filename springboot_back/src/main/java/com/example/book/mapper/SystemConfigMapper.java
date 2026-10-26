package com.example.book.mapper;

import com.example.book.entity.SystemConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SystemConfigMapper {

    List<SystemConfig> findAll();

    SystemConfig findByKey(@Param("configKey") String configKey);
}
