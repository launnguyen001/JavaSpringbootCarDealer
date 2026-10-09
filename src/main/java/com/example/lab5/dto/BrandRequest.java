package com.example.lab5.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrandRequest(
        @NotBlank(message = "Tên hãng xe không được để trống")
        @Size(max = 100, message = "Tên hãng xe không được vượt quá 100 ký tự")
        String name,

        @Size(max = 100, message = "Quốc gia không được vượt quá 100 ký tự")
        String country,

        @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
        String description
) {
}
