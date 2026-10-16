package com.example.lab5.dto;

import com.example.lab5.entity.CarStatus;

public record CarResponse(
        Long id,
        String name,
        String description,
        Double price,
        Integer quantity,
        String vin,
        Integer year,
        String color,
        CarStatus status,
        Long modelId,
        String modelName,
        String brandName
) {
}
