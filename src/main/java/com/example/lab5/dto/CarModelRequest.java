package com.example.lab5.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CarModelRequest(
        @NotBlank(message = "Tên dòng xe không được để trống")
        @Size(max = 100, message = "Tên dòng xe không được vượt quá 100 ký tự")
        String name,

        @Size(max = 50, message = "Kiểu dáng không được vượt quá 50 ký tự")
        String bodyType,

        @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
        String description,

        @NotNull(message = "Hãng xe không được để trống")
        Long brandId
) {
}
