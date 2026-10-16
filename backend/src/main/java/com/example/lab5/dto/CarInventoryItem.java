package com.example.lab5.dto;

/** Một chiếc xe vật lý trong báo cáo tồn kho (drilldown theo dòng xe). */
public record CarInventoryItem(
        Long carId,
        String carName,
        String vin,
        String color,
        Integer year,
        String status,
        String condition,
        Integer mileageKm,
        Double price
) {
}
