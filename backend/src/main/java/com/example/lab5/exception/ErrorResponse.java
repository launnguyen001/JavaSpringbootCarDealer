package com.example.lab5.exception;

public record ErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
