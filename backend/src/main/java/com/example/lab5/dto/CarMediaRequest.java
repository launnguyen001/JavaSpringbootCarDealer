package com.example.lab5.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CarMediaRequest(
        @NotNull(message = "Xe không được để trống")
        Long carId,

        String thumbnail,
        List<String> images,
        List<String> videos
) {
}
