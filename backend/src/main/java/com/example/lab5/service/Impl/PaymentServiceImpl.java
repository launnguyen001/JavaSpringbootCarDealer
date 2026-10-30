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
import com.example.lab5.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/** Payment là giao dịch đã thu; công nợ được suy ra từ tổng giao dịch thành công. */
@Service
public class PaymentServiceImpl implements PaymentService {
    private static final double EPSILON = 0.0001d;
    private final PaymentRepository paymentRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final OrderStateService orderStateService;
    private final NotificationEventFactory notificationEvents;

    public PaymentServiceImpl(PaymentRepository paymentRepository, SalesOrderRepository salesOrderRepository,
                              OrderStateService orderStateService, NotificationEventFactory notificationEvents) {
        this.paymentRepository = paymentRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.orderStateService = orderStateService;
        this.notificationEvents = notificationEvents;
    }

    @Override
    @Transactional
    public Payment create(PaymentRequest request) {
        if (request == null || request.orderId() == null) throw new BadRequestException("Đơn hàng không được để trống");
        if (request.amount() == null || !Double.isFinite(request.amount()) || request.amount() <= 0)
            throw new BadRequestException("Số tiền thanh toán phải lớn hơn 0");
        if (request.method() == null) throw new BadRequestException("Phương thức thanh toán không được để trống");

        SalesOrder order = salesOrderRepository.findByIdForUpdate(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng có id = " + request.orderId()));
        if (order.getStatus() == OrderStatus.CANCELLED) throw new BadRequestException("Không thể thu tiền đơn hàng đã hủy");
        if (order.getStatus() == OrderStatus.COMPLETED) throw new BadRequestException("Đơn hàng đã hoàn tất thanh toán");
        if (order.getTotalAmount() == null || order.getTotalAmount() <= 0)
            throw new BadRequestException("Đơn hàng không có giá trị hợp lệ để thu tiền");

        double paid = paymentRepository.sumPaidAmountByOrderId(order.getId());
        double remaining = Math.max(order.getTotalAmount() - paid, 0d);
        if (request.amount() - remaining > EPSILON)
            throw new BadRequestException("Số tiền vượt quá công nợ còn lại: " + remaining);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(request.amount());
        payment.setMethod(request.method());
        // Giữ cột status để tương thích dữ liệu hiện hữu; mỗi record là khoản tiền đã thu thành công.
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);

        if (paid + saved.getAmount() + EPSILON >= order.getTotalAmount()) {
            orderStateService.transition(order, OrderStatus.COMPLETED,
                    SecurityUtils.currentUsername(), "Đã thu đủ tiền theo các giao dịch");
        }
        notificationEvents.paymentPaid(saved);
        return saved;
    }

    @Override public List<Payment> getAllPayments() { return paymentRepository.findAll(); }

    @Override
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thanh toán có id = " + id));
    }

    @Override
    public List<Payment> getByOrder(Long orderId) {
        if (orderId == null) throw new BadRequestException("Đơn hàng không được để trống");
        return paymentRepository.findByOrder_IdOrderByIdAsc(orderId);
    }

    @Override
    @Transactional
    public Payment update(Long id, PaymentRequest request) {
        getPaymentById(id);
        throw new BadRequestException("Không thể sửa giao dịch đã ghi nhận. Hãy dùng nghiệp vụ hoàn tiền/điều chỉnh riêng.");
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getPaymentById(id);
        throw new BadRequestException("Không thể xóa giao dịch đã ghi nhận để bảo toàn lịch sử kế toán.");
    }
}
