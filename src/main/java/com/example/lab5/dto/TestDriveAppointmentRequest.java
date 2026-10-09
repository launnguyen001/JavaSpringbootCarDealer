package com.example.lab5.dto;

import com.example.lab5.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record TestDriveAppointmentRequest(
        @NotNull(message = "Thời gian hẹn không được để trống")
        LocalDateTime scheduledAt,

        AppointmentStatus status,

        @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
        String note,

        @NotNull(message = "Khách hàng không được để trống")
        Long customerId,

        @NotNull(message = "Xe không được để trống")
        Long carId,

        Long employeeId
) {
}
