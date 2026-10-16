package com.example.lab5.service;

import com.example.lab5.dto.AuthResponse;
import com.example.lab5.dto.UserResponse;

public interface AuthService {

    AuthResponse login(String username, String password);

    void logout();

    UserResponse register(String username, String password, String email, String role);

    UserResponse getCurrentUser();
}
