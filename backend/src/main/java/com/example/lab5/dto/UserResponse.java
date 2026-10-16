package com.example.lab5.dto;

public record UserResponse(
        Long id,
        String username,
        String email,
        String role,
        boolean enabled
) {
}
