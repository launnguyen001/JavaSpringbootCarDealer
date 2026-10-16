package com.example.lab5.dto;

public record EmployeeResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String position
) {
}
