package com.example.lab5.dto;

import com.example.lab5.entity.AppointmentStatus;

import java.time.LocalDateTime;

public record TestDriveAppointmentResponse(
        Long id,
        LocalDateTime scheduledAt,
        AppointmentStatus status,
        String note,
        Long customerId,
        String customerName,
        Long carId,
        String carName,
        Long employeeId,
        String employeeName
) {
}
