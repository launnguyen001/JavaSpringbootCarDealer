package com.example.lab5.repository;

import com.example.lab5.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByOrder_Id(Long orderId);

    boolean existsByOrder_Id(Long orderId);

    /** Hóa đơn mới phát hành gần nhất đứng đầu. */
    List<Invoice> findAllByOrderByIssuedAtDescIdDesc();

    /** Id các đơn hàng đã có hóa đơn. */
    @Query("SELECT i.order.id FROM Invoice i")
    List<Long> findAllOrderIds();
}
