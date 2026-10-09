package com.example.lab5.repository;

import com.example.lab5.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrder_IdOrderByIdAsc(Long orderId);
}
