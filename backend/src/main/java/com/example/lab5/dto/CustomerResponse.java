package com.example.lab5.dto;

public record CustomerResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String address,
        String nationalId
) {
}
