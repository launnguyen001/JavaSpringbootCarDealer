package com.example.lab5.dto;

import java.time.LocalDateTime;

public record ActivityLogResponse(
        String id,
        String action,
        String entityType,
        Long entityId,
        String description,
        String performedBy,
        LocalDateTime timestamp
) {
}
