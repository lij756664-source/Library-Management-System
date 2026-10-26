package com.example.book.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {

    private Integer id;
    private String username;
    private String password;
    private String nickname;
    private String role;
    private LocalDateTime createTime;
}
