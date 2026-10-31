package com.example.lab5.service;

import com.example.lab5.dto.InvoiceRequest;
import com.example.lab5.entity.Invoice;

import java.util.List;

public interface InvoiceService {

    Invoice create(InvoiceRequest request);

    List<Invoice> getAllInvoices(String paymentStatus);

    List<Invoice> getInvoicesForCollection(String paymentStatus);

    Invoice getInvoiceById(Long id);

    Invoice getByOrder(Long orderId);

    Invoice getByVin(String vin);

    Invoice update(Long id, InvoiceRequest request);

    void delete(Long id);
}
