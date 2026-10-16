package com.example.lab5.dto;

import com.example.lab5.entity.PaymentMethod;
import com.example.lab5.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long orderId,
        String orderCode,
        Double amount,
        PaymentMethod method,
        PaymentStatus status,
        LocalDateTime paidAt
) {
}
