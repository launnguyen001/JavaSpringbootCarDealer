package com.example.lab5.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record CarSpecificationRequest(
        @NotNull(message = "Xe không được để trống")
        Long carId,

        String engine,
        String transmission,
        String fuelType,
        Integer seats,
        String dimensions,
        Double weightKg,
        List<String> safetyFeatures,
        Map<String, Object> extra
) {
}
