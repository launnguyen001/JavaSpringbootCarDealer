package com.example.lab5.service.Impl;

import com.example.lab5.dto.PaymentRequest;
import com.example.lab5.entity.Invoice;
import com.example.lab5.entity.OrderStatus;
import com.example.lab5.entity.Payment;
import com.example.lab5.entity.PaymentStatus;
import com.example.lab5.entity.SalesOrder;
import com.example.lab5.event.NotificationEventFactory;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.ContractRepository;
import com.example.lab5.repository.InvoiceRepository;
import com.example.lab5.repository.PaymentRepository;
import com.example.lab5.repository.SalesOrderRepository;
import com.example.lab5.service.OrderStateService;
import com.example.lab5.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Payment lưu từng lần nhận tiền; trạng thái tổng hợp công nợ được quản lý tại Invoice. */
@Service
public class PaymentServiceImpl implements PaymentService {
    private static final double EPSILON = 0.0001d;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final ContractRepository contractRepository;
    private final OrderStateService orderStateService;
    private final NotificationEventFactory notificationEvents;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            SalesOrderRepository salesOrderRepository,
            ContractRepository contractRepository,
            OrderStateService orderStateService,
            NotificationEventFactory notificationEvents
    ) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.contractRepository = contractRepository;
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
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Chỉ được ghi nhận thanh toán sau khi đơn hàng đã CONFIRMED");
        }
        if (order.getTotalAmount() == null || order.getTotalAmount() <= 0)
            throw new BadRequestException("Đơn hàng không có giá trị hợp lệ để thu tiền");

        if (!contractRepository.existsByOrder_Id(order.getId())) {
            throw new BadRequestException("Cần lập hợp đồng trước khi ghi nhận thanh toán");
        }

        Invoice invoice = invoiceRepository.findByOrder_Id(order.getId())
                .orElseThrow(() -> new BadRequestException("Cần lập hóa đơn trước khi ghi nhận thanh toán"));
        if (invoice.getPaymentStatus() == com.example.lab5.entity.InvoicePaymentStatus.CANCELLED) {
            throw new BadRequestException("Không thể thu tiền cho hóa đơn đã hủy");
        }
        double paid = paymentRepository.sumPaidAmountByOrderId(order.getId());
        double remaining = Math.max(invoice.getTotal() - paid, 0d);
        if (request.amount() - remaining > EPSILON)
            throw new BadRequestException("Số tiền vượt quá công nợ còn lại: " + remaining);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(request.amount());
        payment.setMethod(request.method());
        // Một bản ghi được tạo ở đây chỉ đại diện cho tiền đã được nhân viên xác minh là đã nhận.
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);

        double newPaidAmount = paid + saved.getAmount();
        invoice.setPaidAmount(newPaidAmount);
        invoice.refreshPaymentStatus();
        invoiceRepository.save(invoice);

        // Chỉ hoàn tất khi đã thu đủ hóa đơn và hợp đồng đã tồn tại.
        // Nếu hợp đồng được lập sau khi hóa đơn đã thanh toán đủ, ContractService
        // sẽ thực hiện bước chuyển trạng thái còn lại.
        if (invoice.getPaymentStatus() == com.example.lab5.entity.InvoicePaymentStatus.PAID
                && contractRepository.existsByOrder_Id(order.getId())) {
            orderStateService.transition(order, OrderStatus.COMPLETED, "system",
                    "Tự động hoàn tất đơn hàng khi đã thu đủ tiền theo hóa đơn");
        }

        notificationEvents.paymentPaid(saved);
        return saved;
    }

    @Override public List<Payment> getAllPayments() { return paymentRepository.findAllByOrderByPaidAtDescIdDesc(); }

    @Override
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thanh toán có id = " + id));
    }

    @Override
    public List<Payment> getByOrder(Long orderId) {
        if (orderId == null) throw new BadRequestException("Đơn hàng không được để trống");
        return paymentRepository.findByOrder_IdOrderByPaidAtDescIdDesc(orderId);
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
