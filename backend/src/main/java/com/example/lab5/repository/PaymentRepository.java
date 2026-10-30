package com.example.lab5.repository;

import com.example.lab5.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrder_IdOrderByIdAsc(Long orderId);

    boolean existsByOrder_Id(Long orderId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.order.id = :orderId AND p.status IN (com.example.lab5.entity.PaymentStatus.PAID, com.example.lab5.entity.PaymentStatus.PARTIAL)")
    double sumPaidAmountByOrderId(@Param("orderId") Long orderId);
}
