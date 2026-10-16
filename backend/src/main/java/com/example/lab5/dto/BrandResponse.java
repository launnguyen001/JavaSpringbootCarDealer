package com.example.lab5.dto;

public record BrandResponse(
        Long id,
        String name,
        String country,
        String description
) {
}
