package com.example.lab5.repository;

import com.example.lab5.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder_IdOrderByIdAsc(Long orderId);

    boolean existsByCar_Id(Long carId);
}
