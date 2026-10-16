package com.example.lab5.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "Tên đăng nhập không được để trống")
        @Size(max = 100, message = "Tên đăng nhập không được vượt quá 100 ký tự")
        String username,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 6, max = 255, message = "Mật khẩu phải từ 6 đến 255 ký tự")
        String password,

        @Email(message = "Email không hợp lệ")
        @Size(max = 150, message = "Email không được vượt quá 150 ký tự")
        String email,

        String role
) {
}
