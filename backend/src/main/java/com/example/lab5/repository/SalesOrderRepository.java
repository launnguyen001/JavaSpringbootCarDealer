package com.example.lab5.repository;

import com.example.lab5.entity.OrderStatus;
import com.example.lab5.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    Optional<SalesOrder> findByOrderCode(String orderCode);

    List<SalesOrder> findByCustomer_IdOrderByOrderDateDesc(Long customerId);

    List<SalesOrder> findByStatusOrderByOrderDateDesc(OrderStatus status);

    List<SalesOrder> findAllByOrderByOrderDateDesc();

    boolean existsByCustomer_Id(Long customerId);

    boolean existsByEmployee_Id(Long employeeId);
}
