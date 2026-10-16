package com.example.lab5.dto;

import com.example.lab5.entity.CarCondition;

public record OrderItemResponse(
        Long id,
        Long carItemId,
        Long carId,
        String carName,
        String vin,
        CarCondition condition,
        Integer quantity,
        Double unitPrice,
        Double lineTotal
) {
}
