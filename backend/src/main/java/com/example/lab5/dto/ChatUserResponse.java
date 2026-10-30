package com.example.lab5.dto;

/** Thông tin tối thiểu của người dùng có thể bắt đầu cuộc trò chuyện. */
public record ChatUserResponse(Long id, String username, String role) {
}
