package com.example.lab5.service;

import com.example.lab5.dto.PaymentRequest;
import com.example.lab5.entity.Payment;

import java.util.List;

public interface PaymentService {

    Payment create(PaymentRequest request);

    List<Payment> getAllPayments();

    Payment getPaymentById(Long id);

    List<Payment> getByOrder(Long orderId);

    Payment update(Long id, PaymentRequest request);

    void delete(Long id);
}
