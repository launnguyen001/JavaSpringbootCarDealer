package com.example.lab5.mapper;

import com.example.lab5.dto.PaymentResponse;
import com.example.lab5.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder() == null ? null : payment.getOrder().getId(),
                payment.getOrder() == null ? null : payment.getOrder().getOrderCode(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getPaidAt()
        );
    }
}
