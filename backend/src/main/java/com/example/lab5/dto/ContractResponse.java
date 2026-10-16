package com.example.lab5.dto;

import java.time.LocalDateTime;

public record ContractResponse(
        Long id,
        Long orderId,
        String orderCode,
        String contractNumber,
        LocalDateTime signedAt,
        String content
) {
}
