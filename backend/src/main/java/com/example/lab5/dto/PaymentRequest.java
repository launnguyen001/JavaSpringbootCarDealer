package com.example.lab5.dto;

import com.example.lab5.entity.PaymentMethod;
import com.example.lab5.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentRequest(
        @NotNull(message = "Đơn hàng không được để trống")
        Long orderId,

        @NotNull(message = "Số tiền không được để trống")
        @Positive(message = "Số tiền phải lớn hơn 0")
        Double amount,

        @NotNull(message = "Phương thức thanh toán không được để trống")
        PaymentMethod method,

        PaymentStatus status
) {
}
