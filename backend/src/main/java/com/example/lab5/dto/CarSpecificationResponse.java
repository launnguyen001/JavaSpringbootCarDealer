package com.example.lab5.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record CarSpecificationResponse(
        String id,
        Long carId,
        String carName,
        String engine,
        String transmission,
        String fuelType,
        Integer seats,
        String dimensions,
        Double weightKg,
        List<String> safetyFeatures,
        Map<String, Object> extra,
        LocalDateTime updatedAt
) {
}
