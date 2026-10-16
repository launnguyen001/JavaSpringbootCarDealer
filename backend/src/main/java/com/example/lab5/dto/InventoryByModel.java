package com.example.lab5.dto;

public record InventoryByModel(
        Long modelId,
        String modelName,
        String bodyType,
        Long totalCars,
        Long totalQuantity
) {
}
