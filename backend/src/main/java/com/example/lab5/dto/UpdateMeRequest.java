package com.example.lab5.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Người dùng tự cập nhật thông tin ĐĂNG NHẬP của mình (email / mật khẩu).
 * Bỏ trống trường nào thì giữ nguyên trường đó.
 */
public record UpdateMeRequest(
        @Email(message = "Email không hợp lệ")
        @Size(max = 150, message = "Email không được vượt quá 150 ký tự")
        String email,

        @Size(min = 4, max = 100, message = "Mật khẩu cần từ 4 đến 100 ký tự")
        String password
) {
}
