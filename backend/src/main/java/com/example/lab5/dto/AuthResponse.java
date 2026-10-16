package com.example.lab5.dto;

public record AuthResponse(
        String token,
        String username,
        String role,
        long expiresIn
) {
}
