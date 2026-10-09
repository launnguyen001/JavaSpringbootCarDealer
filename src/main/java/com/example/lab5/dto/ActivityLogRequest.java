package com.example.lab5.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record ActivityLogRequest(
        @NotBlank(message = "Hành động không được để trống")
        String action,

        String entityType,

        Long entityId,

        String description,

        String performedBy,

        LocalDateTime timestamp
) {
}
