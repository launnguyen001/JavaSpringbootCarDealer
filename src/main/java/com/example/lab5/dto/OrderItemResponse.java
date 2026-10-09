package com.example.lab5.dto;

public record OrderItemResponse(
        Long id,
        Long carId,
        String carName,
        Integer quantity,
        Double unitPrice,
        Double lineTotal
) {
}
