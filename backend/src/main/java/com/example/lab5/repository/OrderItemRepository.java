package com.example.lab5.repository;

import com.example.lab5.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder_IdOrderByIdAsc(Long orderId);

    java.util.Optional<OrderItem> findFirstByCarItem_IdOrderByIdDesc(Long carItemId);

    boolean existsByCarItem_Id(Long carItemId);

    // Có giao dịch nào gắn với bất kỳ chiếc xe nào của dòng xe này không
    boolean existsByCarItem_Car_Id(Long carId);
}
