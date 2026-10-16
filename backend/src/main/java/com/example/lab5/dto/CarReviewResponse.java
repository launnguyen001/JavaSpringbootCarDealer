package com.example.lab5.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CarReviewResponse(
        String id,
        Long carId,
        Long customerId,
        String customerName,
        Integer rating,
        String title,
        String content,
        LocalDateTime createdAt,
        List<Reply> replies
) {
    public record Reply(
            String author,
            String content,
            LocalDateTime createdAt
    ) {
    }
}
