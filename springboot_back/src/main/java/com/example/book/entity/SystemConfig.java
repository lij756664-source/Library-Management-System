package com.example.book.entity;

import lombok.Data;

@Data
public class SystemConfig {

    private Integer id;
    private String configKey;
    private String configValue;
    private String description;
}
