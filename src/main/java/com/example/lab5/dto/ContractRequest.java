package com.example.lab5.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ContractRequest(
        @NotNull(message = "Đơn hàng không được để trống")
        Long orderId,

        @NotBlank(message = "Số hợp đồng không được để trống")
        @Size(max = 40, message = "Số hợp đồng không được vượt quá 40 ký tự")
        String contractNumber,

        LocalDateTime signedAt,

        @Size(max = 2000, message = "Nội dung không được vượt quá 2000 ký tự")
        String content
) {
}
