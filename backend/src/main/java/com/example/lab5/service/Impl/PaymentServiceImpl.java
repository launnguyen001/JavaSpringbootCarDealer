package com.example.lab5.service.Impl;

import com.example.lab5.dto.PaymentRequest;
import com.example.lab5.entity.OrderStatus;
import com.example.lab5.entity.Payment;
import com.example.lab5.entity.PaymentStatus;
import com.example.lab5.entity.SalesOrder;
import com.example.lab5.event.NotificationEventFactory;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.PaymentRepository;
import com.example.lab5.repository.SalesOrderRepository;
import com.example.lab5.service.OrderStateService;
import com.example.lab5.service.PaymentService;
import com.example.lab5.statemachine.PaymentStateMachine;
import com.example.lab5.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final OrderStateService orderStateService;
    private final PaymentStateMachine paymentStateMachine;
    private final NotificationEventFactory notificationEvents;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            SalesOrderRepository salesOrderRepository,
            OrderStateService orderStateService,
            PaymentStateMachine paymentStateMachine,
            NotificationEventFactory notificationEvents
    ) {
        this.paymentRepository = paymentRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.orderStateService = orderStateService;
        this.paymentStateMachine = paymentStateMachine;
        this.notificationEvents = notificationEvents;
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
            orderStateService.transition(order, OrderStatus.COMPLETED,
                    SecurityUtils.currentUsername(), "Thanh toán đủ");
        }
        Payment saved = paymentRepository.save(payment);
        if (saved.getStatus() == PaymentStatus.PAID) {
            notificationEvents.paymentPaid(saved);
        }
        return saved;
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

    @Override
    @Transactional
    public Payment update(Long id, PaymentRequest request) {
        if (request == null) {
            throw new BadRequestException("Dữ liệu thanh toán không được để trống");
        }
        if (request.amount() == null || request.amount() <= 0) {
            throw new BadRequestException("Số tiền thanh toán phải lớn hơn 0");
        }
        if (request.method() == null) {
            throw new BadRequestException("Phương thức thanh toán không được để trống");
        }

        Payment payment = getPaymentById(id);
        PaymentStatus currentStatus = payment.getStatus();
        PaymentStatus status = request.status() == null ? currentStatus : request.status();
        // Chỉ cho phép chuyển trạng thái thanh toán theo đúng luồng (không quay lại UNPAID sau PAID…)
        paymentStateMachine.assertCanTransition(currentStatus, status);

        payment.setAmount(request.amount());
        payment.setMethod(request.method());
        payment.setStatus(status);

        boolean justPaid = status == PaymentStatus.PAID && currentStatus != PaymentStatus.PAID;
        if (justPaid) {
            payment.setPaidAt(LocalDateTime.now());
            orderStateService.transition(payment.getOrder(), OrderStatus.COMPLETED,
                    SecurityUtils.currentUsername(), "Thanh toán đủ");
        }
        Payment saved = paymentRepository.save(payment);
        if (justPaid) {
            notificationEvents.paymentPaid(saved);
        }
        return saved;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getPaymentById(id);
        paymentRepository.deleteById(id);
    }
}
