package com.example.lab5.dto;

public record TopSellingCar(
        Long carId,
        String carName,
        Long totalSold,
        Double revenue
) {
}
