package com.example.lab5.mapper;

import com.example.lab5.dto.OrderItemResponse;
import com.example.lab5.dto.SalesOrderResponse;
import com.example.lab5.entity.CarItem;
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

        CarItem carItem = item.getCarItem();
        Long carId = null;
        String carName = null;
        if (carItem != null && carItem.getCar() != null) {
            carId = carItem.getCar().getId();
            carName = carItem.getCar().getName();
        }

        return new OrderItemResponse(
                item.getId(),
                carItem == null ? null : carItem.getId(),
                carId,
                carName,
                carItem == null ? null : carItem.getVin(),
                carItem == null ? null : carItem.getCondition(),
                item.getQuantity(),
                item.getUnitPrice(),
                lineTotal
        );
    }
}
