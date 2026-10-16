package com.example.lab5.dto;

import com.example.lab5.entity.CarCondition;
import com.example.lab5.entity.CarStatus;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Form tạo/sửa MỘT chiếc xe vật lý (car_items). */
public record CarItemRequest(
        @Size(max = 50, message = "Số VIN không được vượt quá 50 ký tự")
        String vin,

        @Size(max = 50, message = "Màu xe không được vượt quá 50 ký tự")
        String color,

        Integer year,

        // Bỏ trống = NEW
        CarCondition condition,

        @PositiveOrZero(message = "Giá bán không hợp lệ")
        Double price,

        // Bỏ trống = AVAILABLE
        CarStatus status,

        @PositiveOrZero(message = "Số km không hợp lệ")
        Integer mileageKm,

        @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
        String note
) {
}
