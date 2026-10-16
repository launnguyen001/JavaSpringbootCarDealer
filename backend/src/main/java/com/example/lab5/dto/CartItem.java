package com.example.lab5.dto;

// Giỏ cấu hình (Redis) giờ trỏ tới chiếc xe cụ thể
public record CartItem(
        Long carItemId,
        Integer quantity
) {
}
