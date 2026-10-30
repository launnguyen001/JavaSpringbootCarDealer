package com.example.lab5.dto;

import com.example.lab5.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        String entityType,
        Long entityId,
        boolean read,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}
