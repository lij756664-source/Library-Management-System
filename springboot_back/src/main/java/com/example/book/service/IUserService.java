package com.example.book.service;

import com.example.book.entity.User;

public interface IUserService {

    User login(String username, String password);
}
