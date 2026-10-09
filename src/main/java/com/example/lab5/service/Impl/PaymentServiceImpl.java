package com.example.lab5.service.Impl;

import com.example.lab5.dto.PaymentRequest;
import com.example.lab5.entity.OrderStatus;
import com.example.lab5.entity.Payment;
import com.example.lab5.entity.PaymentStatus;
import com.example.lab5.entity.SalesOrder;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.PaymentRepository;
import com.example.lab5.repository.SalesOrderRepository;
import com.example.lab5.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final SalesOrderRepository salesOrderRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository, SalesOrderRepository salesOrderRepository) {
        this.paymentRepository = paymentRepository;
        this.salesOrderRepository = salesOrderRepository;
    }

    @Override
    @Transactional
    public Payment create(PaymentRequest request) {
        if (request == null || request.orderId() == null) {
            throw new BadRequestException("Đơn hàng không được để trống");
        }
        if (request.amount() == null || request.amount() <= 0) {
            throw new BadRequestException("Số tiền thanh toán phải lớn hơn 0");
        }
        if (request.method() == null) {
            throw new BadRequestException("Phương thức thanh toán không được để trống");
        }

        SalesOrder order = salesOrderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn hàng có id = " + request.orderId()));

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(request.amount());
        payment.setMethod(request.method());
        payment.setStatus(request.status() == null ? PaymentStatus.PAID : request.status());
        if (payment.getStatus() == PaymentStatus.PAID) {
            payment.setPaidAt(LocalDateTime.now());
            order.setStatus(OrderStatus.COMPLETED);
            salesOrderRepository.save(order);
        }
        return paymentRepository.save(payment);
    }

    @Override
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @Override
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thanh toán có id = " + id));
    }

    @Override
    public List<Payment> getByOrder(Long orderId) {
        if (orderId == null) {
            throw new BadRequestException("Đơn hàng không được để trống");
        }
        return paymentRepository.findByOrder_IdOrderByIdAsc(orderId);
    }
}
