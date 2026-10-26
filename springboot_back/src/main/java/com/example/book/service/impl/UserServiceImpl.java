package com.example.book.service.impl;

import com.example.book.common.BusinessException;
import com.example.book.entity.User;
import com.example.book.mapper.UserMapper;
import com.example.book.service.IUserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements IUserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public User login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null || !user.getPassword().equals(password)) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        user.setPassword(null);
        return user;
    }
}
