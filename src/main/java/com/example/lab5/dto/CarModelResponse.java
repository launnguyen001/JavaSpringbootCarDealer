package com.example.lab5.dto;

public record CarModelResponse(
        Long id,
        String name,
        String bodyType,
        String description,
        Long brandId,
        String brandName
) {
}
