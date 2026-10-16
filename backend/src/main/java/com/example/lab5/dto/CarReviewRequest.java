package com.example.lab5.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CarReviewRequest(
        @NotNull(message = "Xe không được để trống")
        Long carId,

        Long customerId,

        String customerName,

        @NotNull(message = "Điểm đánh giá không được để trống")
        @Min(value = 1, message = "Điểm đánh giá tối thiểu là 1")
        @Max(value = 5, message = "Điểm đánh giá tối đa là 5")
        Integer rating,

        String title,
        String content
) {
}
