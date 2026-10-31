package com.example.lab5.dto;

import java.util.List;

/** Tổng hợp thông tin vật lý, thông số MongoDB và chứng từ/giao dịch của một VIN. */
public record CarItemDetailResponse(
        CarItemResponse vehicle,
        CarSpecificationResponse specifications,
        OrderSummary relatedOrder,
        InvoiceResponse invoice,
        ContractResponse contract,
        List<PaymentResponse> payments
) {
}
