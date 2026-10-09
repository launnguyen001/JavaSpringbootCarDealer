package com.example.lab5.dto;

public record InventoryByBrand(
        Long brandId,
        String brandName,
        Long totalCars,
        Long totalQuantity
) {
}
