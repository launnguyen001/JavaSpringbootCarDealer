package com.example.lab5.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank(message = "Tên đăng nhập không được để trống") String username,
        @NotBlank(message = "Mật khẩu không được để trống") String password
) {
}
