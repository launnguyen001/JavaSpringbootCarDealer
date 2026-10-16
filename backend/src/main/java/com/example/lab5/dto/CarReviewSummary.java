package com.example.lab5.dto;

public record CarReviewSummary(
        Long carId,
        long totalReviews,
        double averageRating
) {
}
