package com.example.lab5.controller;

import com.example.lab5.dto.InvoiceRequest;
import com.example.lab5.dto.InvoiceResponse;
import com.example.lab5.entity.Invoice;
import com.example.lab5.mapper.InvoiceMapper;
import com.example.lab5.service.InvoiceService;
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
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceMapper invoiceMapper;

    public InvoiceController(InvoiceService invoiceService, InvoiceMapper invoiceMapper) {
        this.invoiceService = invoiceService;
        this.invoiceMapper = invoiceMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse createInvoice(@Valid @RequestBody InvoiceRequest request) {
        return invoiceMapper.toResponse(invoiceService.create(request));
    }

    @GetMapping
    public List<InvoiceResponse> getAllInvoices() {
        return toResponses(invoiceService.getAllInvoices());
    }

    @GetMapping("/{id}")
    public InvoiceResponse getInvoiceById(@PathVariable Long id) {
        return invoiceMapper.toResponse(invoiceService.getInvoiceById(id));
    }

    @GetMapping("/order/{orderId}")
    public InvoiceResponse getByOrder(@PathVariable Long orderId) {
        return invoiceMapper.toResponse(invoiceService.getByOrder(orderId));
    }

    @PutMapping("/{id}")
    public InvoiceResponse updateInvoice(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceRequest request
    ) {
        return invoiceMapper.toResponse(invoiceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvoice(@PathVariable Long id) {
        invoiceService.delete(id);
    }

    private List<InvoiceResponse> toResponses(List<Invoice> invoices) {
        return invoices.stream().map(invoiceMapper::toResponse).toList();
    }
}
