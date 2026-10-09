package com.example.lab5.dto;

import com.example.lab5.entity.CarStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CarRequest(
        @NotBlank(message = "Tên xe không được để trống")
        @Size(max = 100, message = "Tên xe không được vượt quá 100 ký tự")
        String name,

        @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
        String description,

        @NotNull(message = "Giá xe không được để trống")
        @PositiveOrZero(message = "Giá xe không hợp lệ")
        Double price,

        @NotNull(message = "Số lượng không được để trống")
        @PositiveOrZero(message = "Số lượng không hợp lệ")
        Integer quantity,

        @Size(max = 50, message = "Số VIN không được vượt quá 50 ký tự")
        String vin,

        Integer year,

        @Size(max = 50, message = "Màu xe không được vượt quá 50 ký tự")
        String color,

        CarStatus status,

        @NotNull(message = "Dòng xe không được để trống")
        Long modelId
) {
}
