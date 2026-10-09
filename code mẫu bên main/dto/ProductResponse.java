package com.example.lab5.dto;

public record ProductResponse(
        Long id,
        String name,
        String description,
        Double price,
        Integer quantity,
        Long categoryId
) {
}
