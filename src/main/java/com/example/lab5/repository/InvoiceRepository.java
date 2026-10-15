package com.example.lab5.repository;

import com.example.lab5.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByOrder_Id(Long orderId);

    boolean existsByOrder_Id(Long orderId);
}
