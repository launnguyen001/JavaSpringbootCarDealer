package com.example.lab5.dto;

import jakarta.validation.constraints.NotBlank;

/** Admin đổi vai trò của một tài khoản (ADMIN / EMPLOYEE / USER). */
public record UserRoleRequest(
        @NotBlank(message = "Vai trò không được để trống")
        String role
) {
}
