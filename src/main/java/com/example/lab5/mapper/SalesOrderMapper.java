package com.example.lab5.mapper;

import com.example.lab5.dto.OrderItemResponse;
import com.example.lab5.dto.SalesOrderResponse;
import com.example.lab5.entity.OrderItem;
import com.example.lab5.entity.SalesOrder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SalesOrderMapper {

    public SalesOrderResponse toResponse(SalesOrder order) {
        List<OrderItemResponse> items = order.getItems() == null
                ? List.of()
                : order.getItems().stream().map(this::toItemResponse).toList();

        return new SalesOrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getOrderDate(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCustomer() == null ? null : order.getCustomer().getId(),
                order.getCustomer() == null ? null : order.getCustomer().getFullName(),
                order.getEmployee() == null ? null : order.getEmployee().getId(),
                order.getEmployee() == null ? null : order.getEmployee().getFullName(),
                items
        );
    }

    public OrderItemResponse toItemResponse(OrderItem item) {
        Double lineTotal = item.getUnitPrice() == null || item.getQuantity() == null
                ? null
                : item.getUnitPrice() * item.getQuantity();
        return new OrderItemResponse(
                item.getId(),
                item.getCar() == null ? null : item.getCar().getId(),
                item.getCar() == null ? null : item.getCar().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                lineTotal
        );
    }
}
