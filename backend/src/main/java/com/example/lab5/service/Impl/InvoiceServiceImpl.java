package com.example.lab5.service.Impl;

import com.example.lab5.dto.InvoiceRequest;
import com.example.lab5.entity.Invoice;
import com.example.lab5.entity.SalesOrder;
import com.example.lab5.event.NotificationEventFactory;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.InvoiceRepository;
import com.example.lab5.repository.SalesOrderRepository;
import com.example.lab5.service.InvoiceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final NotificationEventFactory notificationEvents;

    public InvoiceServiceImpl(
            InvoiceRepository invoiceRepository,
            SalesOrderRepository salesOrderRepository,
            NotificationEventFactory notificationEvents
    ) {
        this.invoiceRepository = invoiceRepository;
        this.salesOrderRepository = salesOrderRepository;
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
        if (request.total() == null || request.total() < 0) {
            throw new BadRequestException("Tổng tiền hóa đơn không hợp lệ");
        }
        if (invoiceRepository.findByOrder_Id(request.orderId()).isPresent()) {
            throw new BadRequestException("Đơn hàng đã có hóa đơn");
        }

        SalesOrder order = salesOrderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn hàng có id = " + request.orderId()));

        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setInvoiceNumber(request.invoiceNumber().trim());
        invoice.setTotal(request.total());
        invoice.setIssuedAt(request.issuedAt() == null ? LocalDateTime.now() : request.issuedAt());
        Invoice saved = invoiceRepository.save(invoice);
        notificationEvents.invoiceCreated(saved);
        return saved;
    }

    @Override
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    @Override
    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn có id = " + id));
    }

    @Override
    public Invoice getByOrder(Long orderId) {
        return invoiceRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hóa đơn của đơn hàng id = " + orderId));
    }

    @Override
    @Transactional
    public Invoice update(Long id, InvoiceRequest request) {
        if (request == null || request.invoiceNumber() == null || request.invoiceNumber().isBlank()) {
            throw new BadRequestException("Số hóa đơn không được để trống");
        }
        if (request.total() == null || request.total() < 0) {
            throw new BadRequestException("Tổng tiền hóa đơn không hợp lệ");
        }
        Invoice invoice = getInvoiceById(id);
        String invoiceNumber = request.invoiceNumber().trim();
        invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Số hóa đơn đã tồn tại");
                });
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setTotal(request.total());
        if (request.issuedAt() != null) {
            invoice.setIssuedAt(request.issuedAt());
        }
        return invoiceRepository.save(invoice);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getInvoiceById(id);
        invoiceRepository.deleteById(id);
    }
}
