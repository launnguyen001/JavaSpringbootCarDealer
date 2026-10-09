package com.example.lab5.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SalesOrderRequest(
        @NotNull(message = "Khách hàng không được để trống")
        Long customerId,

        Long employeeId,

        @NotEmpty(message = "Đơn hàng phải có ít nhất một xe")
        @Valid
        List<OrderItemRequest> items
) {
}
