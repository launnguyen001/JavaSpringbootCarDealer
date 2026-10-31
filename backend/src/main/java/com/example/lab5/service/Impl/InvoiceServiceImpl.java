package com.example.lab5.service.Impl;

import com.example.lab5.dto.InvoiceRequest;
import com.example.lab5.entity.Invoice;
import com.example.lab5.entity.InvoicePaymentStatus;
import com.example.lab5.entity.OrderStatus;
import com.example.lab5.entity.SalesOrder;
import com.example.lab5.event.NotificationEventFactory;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.InvoiceRepository;
import com.example.lab5.repository.PaymentRepository;
import com.example.lab5.repository.SalesOrderRepository;
import com.example.lab5.service.InvoiceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationEventFactory notificationEvents;

    public InvoiceServiceImpl(
            InvoiceRepository invoiceRepository,
            SalesOrderRepository salesOrderRepository,
            PaymentRepository paymentRepository,
            NotificationEventFactory notificationEvents
    ) {
        this.invoiceRepository = invoiceRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.paymentRepository = paymentRepository;
        this.notificationEvents = notificationEvents;
    }

    @Override
    @Transactional
    public Invoice create(InvoiceRequest request) {
        if (request == null || request.orderId() == null) {
            throw new BadRequestException("Đơn hàng không được để trống");
        }
        if (request.invoiceNumber() == null || request.invoiceNumber().isBlank()) {
            throw new BadRequestException("Số hóa đơn không được để trống");
        }
        if (request.total() == null || request.total() <= 0 || !Double.isFinite(request.total())) {
            throw new BadRequestException("Tổng tiền hóa đơn không hợp lệ");
        }
        SalesOrder order = salesOrderRepository.findByIdForUpdate(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn hàng có id = " + request.orderId()));
        if (invoiceRepository.findByOrder_Id(request.orderId()).isPresent()) {
            throw new BadRequestException("Đơn hàng đã có hóa đơn");
        }
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Chỉ được lập hóa đơn sau khi đơn hàng đã CONFIRMED");
        }
        if (Math.abs(request.total() - order.getTotalAmount()) > 0.0001d) {
            throw new BadRequestException("Tổng tiền hóa đơn phải bằng giá trị đơn hàng: " + order.getTotalAmount());
        }

        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setInvoiceNumber(request.invoiceNumber().trim());
        invoice.setTotal(order.getTotalAmount());
        invoice.setIssuedAt(request.issuedAt() == null ? LocalDateTime.now() : request.issuedAt());
        invoice.setDueDate(request.dueDate());
        // Thanh toán có thể được ghi nhận trước khi hóa đơn được lập.
        invoice.setPaidAmount(paymentRepository.sumPaidAmountByOrderId(order.getId()));
        invoice.refreshPaymentStatus();

        Invoice saved = invoiceRepository.save(invoice);
        notificationEvents.invoiceCreated(saved);
        return saved;
    }

    @Override
    @Transactional
    public List<Invoice> getAllInvoices(String paymentStatus) {
        List<Invoice> invoices = invoiceRepository.findAllByOrderByIssuedAtDescIdDesc();
        invoices.forEach(invoice -> {
            invoice.setPaidAmount(paymentRepository.sumPaidAmountByOrderId(invoice.getOrder().getId()));
            invoice.refreshPaymentStatus();
        });
        if (paymentStatus == null || paymentStatus.isBlank()) return invoices;
        InvoicePaymentStatus status = parseStatus(paymentStatus);
        return invoices.stream().filter(invoice -> invoice.getPaymentStatus() == status).toList();
    }

    @Override
    @Transactional
    public List<Invoice> getInvoicesForCollection(String paymentStatus) {
        List<Invoice> invoices = getAllInvoices(paymentStatus);
        return invoices.stream()
                .filter(invoice -> invoice.getPaymentStatus() != InvoicePaymentStatus.PAID
                        && invoice.getPaymentStatus() != InvoicePaymentStatus.CANCELLED)
                .toList();
    }

    @Override
    public Invoice getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn có id = " + id));
        syncPaymentTotal(invoice);
        return invoice;
    }

    @Override
    public Invoice getByOrder(Long orderId) {
        Invoice invoice = invoiceRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hóa đơn của đơn hàng id = " + orderId));
        syncPaymentTotal(invoice);
        return invoice;
    }

    @Override
    public Invoice getByOrderCode(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) {
            throw new BadRequestException("Mã đơn hàng không được để trống");
        }
        Invoice invoice = invoiceRepository.findByOrder_OrderCode(orderCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hóa đơn của đơn hàng mã = " + orderCode));
        syncPaymentTotal(invoice);
        return invoice;
    }

    @Override
    public Invoice getByVin(String vin) {
        if (vin == null || vin.isBlank()) throw new BadRequestException("Mã VIN không được để trống");
        Invoice invoice = invoiceRepository.findAllByVehicleVin(vin.trim()).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn theo VIN = " + vin));
        syncPaymentTotal(invoice);
        return invoice;
    }

    @Override
    @Transactional
    public Invoice update(Long id, InvoiceRequest request) {
        if (request == null || request.invoiceNumber() == null || request.invoiceNumber().isBlank()) {
            throw new BadRequestException("Số hóa đơn không được để trống");
        }
        Invoice invoice = getInvoiceById(id);
        String invoiceNumber = request.invoiceNumber().trim();
        invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Số hóa đơn đã tồn tại");
                });
        if (request.total() == null || Math.abs(request.total() - invoice.getOrder().getTotalAmount()) > 0.0001d) {
            throw new BadRequestException("Tổng tiền hóa đơn phải bằng giá trị đơn hàng: " + invoice.getOrder().getTotalAmount());
        }
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setTotal(invoice.getOrder().getTotalAmount());
        if (request.issuedAt() != null) invoice.setIssuedAt(request.issuedAt());
        if (request.dueDate() != null) invoice.setDueDate(request.dueDate());
        syncPaymentTotal(invoice);
        return invoiceRepository.save(invoice);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Invoice invoice = getInvoiceById(id);
        if (paymentRepository.existsByOrder_Id(invoice.getOrder().getId())) {
            throw new BadRequestException("Không thể xóa hóa đơn đã phát sinh thanh toán; hãy dùng nghiệp vụ điều chỉnh/hủy hóa đơn");
        }
        invoiceRepository.delete(invoice);
    }

    private void syncPaymentTotal(Invoice invoice) {
        invoice.setPaidAmount(paymentRepository.sumPaidAmountByOrderId(invoice.getOrder().getId()));
        invoice.refreshPaymentStatus();
    }

    private InvoicePaymentStatus parseStatus(String value) {
        try {
            return InvoicePaymentStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Trạng thái thanh toán hóa đơn không hợp lệ: " + value);
        }
    }
}
