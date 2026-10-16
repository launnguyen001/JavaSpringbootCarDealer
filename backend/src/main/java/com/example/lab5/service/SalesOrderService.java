package com.example.lab5.service;

import com.example.lab5.dto.OrderItemRequest;
import com.example.lab5.dto.OrderOptionResponse;
import com.example.lab5.dto.SalesOrderRequest;
import com.example.lab5.entity.OrderItem;
import com.example.lab5.entity.OrderStatus;
import com.example.lab5.entity.SalesOrder;

import java.util.List;

public interface SalesOrderService {

    SalesOrder create(SalesOrderRequest request);

    List<SalesOrder> getAllOrders();

    SalesOrder getOrderById(Long id);

    List<SalesOrder> getByCustomer(Long customerId);

    /**
     * Danh sách đơn hàng đủ điều kiện gắn chứng từ, phục vụ dropdown ở form admin.
     *
     * @param target loại chứng từ: {@code invoice}, {@code contract} hoặc {@code payment}
     */
    List<OrderOptionResponse> getOrderOptions(String target);

    SalesOrder updateStatus(Long id, OrderStatus status);

    SalesOrder cancel(Long id);

    SalesOrder update(Long id, SalesOrderRequest request);

    void delete(Long id);

    List<OrderItem> getItems(Long orderId);

    OrderItem addItem(Long orderId, OrderItemRequest request);

    OrderItem updateItem(Long orderId, Long itemId, OrderItemRequest request);

    void deleteItem(Long orderId, Long itemId);
}
