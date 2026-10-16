package com.example.lab5.dto;

import com.example.lab5.entity.CarCondition;
import com.example.lab5.entity.CarStatus;

/** Một chiếc xe vật lý trả về cho client. */
public record CarItemResponse(
        Long id,
        Long carId,
        String carName,
        String vin,
        String color,
        Integer year,
        CarCondition condition,
        Double price,
        CarStatus status,
        Integer mileageKm,
        String note,
        Long modelId,
        String modelName,
        String brandName
) {
}
