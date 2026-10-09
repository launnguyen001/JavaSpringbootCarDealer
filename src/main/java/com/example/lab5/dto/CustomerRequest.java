package com.example.lab5.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "Họ tên khách hàng không được để trống")
        @Size(max = 150, message = "Họ tên không được vượt quá 150 ký tự")
        String fullName,

        @Email(message = "Email không hợp lệ")
        @Size(max = 150, message = "Email không được vượt quá 150 ký tự")
        String email,

        @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
        String phone,

        @Size(max = 300, message = "Địa chỉ không được vượt quá 300 ký tự")
        String address,

        @Size(max = 30, message = "CMND/CCCD không được vượt quá 30 ký tự")
        String nationalId
) {
}
