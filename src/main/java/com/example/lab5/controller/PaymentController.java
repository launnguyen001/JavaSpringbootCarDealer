package com.example.lab5.controller;

import com.example.lab5.dto.PaymentRequest;
import com.example.lab5.dto.PaymentResponse;
import com.example.lab5.entity.Payment;
import com.example.lab5.mapper.PaymentMapper;
import com.example.lab5.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    public PaymentController(PaymentService paymentService, PaymentMapper paymentMapper) {
        this.paymentService = paymentService;
        this.paymentMapper = paymentMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(@Valid @RequestBody PaymentRequest request) {
        return paymentMapper.toResponse(paymentService.create(request));
    }

    @GetMapping
    public List<PaymentResponse> getAllPayments() {
        return toResponses(paymentService.getAllPayments());
    }

    @GetMapping("/{id}")
    public PaymentResponse getPaymentById(@PathVariable Long id) {
        return paymentMapper.toResponse(paymentService.getPaymentById(id));
    }

    @GetMapping("/order/{orderId}")
    public List<PaymentResponse> getByOrder(@PathVariable Long orderId) {
        return toResponses(paymentService.getByOrder(orderId));
    }

    @PutMapping("/{id}")
    public PaymentResponse updatePayment(@PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
        return paymentMapper.toResponse(paymentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePayment(@PathVariable Long id) {
        paymentService.delete(id);
    }

    private List<PaymentResponse> toResponses(List<Payment> payments) {
        return payments.stream().map(paymentMapper::toResponse).toList();
    }
}
