package com.example.lab5.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record InvoiceRequest(
        @NotNull(message = "Đơn hàng không được để trống")
        Long orderId,

        @NotBlank(message = "Số hóa đơn không được để trống")
        @Size(max = 40, message = "Số hóa đơn không được vượt quá 40 ký tự")
        String invoiceNumber,

        @NotNull(message = "Tổng tiền không được để trống")
        @PositiveOrZero(message = "Tổng tiền không hợp lệ")
        Double total,

        LocalDateTime issuedAt,
        LocalDate dueDate
) {
}
