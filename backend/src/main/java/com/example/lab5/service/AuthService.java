package com.example.lab5.service;

import com.example.lab5.dto.AuthResponse;
import com.example.lab5.dto.UserResponse;

public interface AuthService {

    AuthResponse login(String username, String password);

    void logout();

    // Đăng ký công khai: luôn tạo tài khoản role USER (không nhận role từ client)
    UserResponse register(String username, String password, String email);

    UserResponse getCurrentUser();
}
