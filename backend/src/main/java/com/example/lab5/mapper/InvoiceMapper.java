package com.example.lab5.mapper;

import com.example.lab5.dto.InvoiceResponse;
import com.example.lab5.entity.Invoice;
import org.springframework.stereotype.Component;

@Component
public class InvoiceMapper {

    public InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getOrder() == null ? null : invoice.getOrder().getId(),
                invoice.getOrder() == null ? null : invoice.getOrder().getOrderCode(),
                invoice.getInvoiceNumber(),
                invoice.getIssuedAt(),
                invoice.getTotal()
        );
    }
}
