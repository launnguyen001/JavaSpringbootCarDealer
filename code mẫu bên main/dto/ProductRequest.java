package com.example.lab5.dto;

public record ProductRequest(
        String name,
        String description,
        Double price,
        Integer quantity,
        Long categoryId
) {
}
