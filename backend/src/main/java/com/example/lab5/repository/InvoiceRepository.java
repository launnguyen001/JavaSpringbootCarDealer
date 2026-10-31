package com.example.lab5.repository;

import com.example.lab5.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByOrder_Id(Long orderId);

    Optional<Invoice> findByOrder_OrderCode(String orderCode);

    @Query("SELECT DISTINCT i FROM Invoice i JOIN i.order o JOIN o.items oi JOIN oi.carItem ci WHERE UPPER(ci.vin) = UPPER(:vin) ORDER BY i.issuedAt DESC, i.id DESC")
    List<Invoice> findAllByVehicleVin(@org.springframework.data.repository.query.Param("vin") String vin);

    boolean existsByOrder_Id(Long orderId);

    /** Hóa đơn mới phát hành gần nhất đứng đầu. */
    List<Invoice> findAllByOrderByIssuedAtDescIdDesc();

    /** Id các đơn hàng đã có hóa đơn. */
    @Query("SELECT i.order.id FROM Invoice i")
    List<Long> findAllOrderIds();
}
