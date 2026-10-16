package com.example.lab5.dto;

public record CarInventoryItem(
        Long carId,
        String carName,
        String vin,
        String color,
        Integer year,
        String status,
        Integer quantity,
        Double price
) {
}
