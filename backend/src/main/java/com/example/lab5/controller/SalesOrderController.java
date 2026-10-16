package com.example.lab5.controller;

import com.example.lab5.dto.OrderItemRequest;
import com.example.lab5.dto.OrderItemResponse;
import com.example.lab5.dto.OrderOptionResponse;
import com.example.lab5.dto.SalesOrderRequest;
import com.example.lab5.dto.SalesOrderResponse;
import com.example.lab5.entity.OrderStatus;
import com.example.lab5.entity.SalesOrder;
import com.example.lab5.mapper.SalesOrderMapper;
import com.example.lab5.service.SalesOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;
    private final SalesOrderMapper salesOrderMapper;

    public SalesOrderController(SalesOrderService salesOrderService, SalesOrderMapper salesOrderMapper) {
        this.salesOrderService = salesOrderService;
        this.salesOrderMapper = salesOrderMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalesOrderResponse createOrder(@Valid @RequestBody SalesOrderRequest request) {
        return salesOrderMapper.toResponse(salesOrderService.create(request));
    }

    @GetMapping
    public List<SalesOrderResponse> getAllOrders() {
        return toResponses(salesOrderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public SalesOrderResponse getOrderById(@PathVariable Long id) {
        return salesOrderMapper.toResponse(salesOrderService.getOrderById(id));
    }

    /**
     * Danh sách đơn hàng đủ điều kiện gắn chứng từ, dùng cho dropdown ở form admin.
     * target = invoice | contract | payment
     */
    @GetMapping("/options")
    public List<OrderOptionResponse> getOrderOptions(@RequestParam String target) {
        return salesOrderService.getOrderOptions(target);
    }

    @GetMapping("/customer/{customerId}")
    public List<SalesOrderResponse> getByCustomer(@PathVariable Long customerId) {
        return toResponses(salesOrderService.getByCustomer(customerId));
    }

    @PatchMapping("/{id}/status")
    public SalesOrderResponse updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status
    ) {
        return salesOrderMapper.toResponse(salesOrderService.updateStatus(id, status));
    }

    @PostMapping("/{id}/cancel")
    public SalesOrderResponse cancelOrder(@PathVariable Long id) {
        return salesOrderMapper.toResponse(salesOrderService.cancel(id));
    }

    @PutMapping("/{id}")
    public SalesOrderResponse updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody SalesOrderRequest request
    ) {
        return salesOrderMapper.toResponse(salesOrderService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(@PathVariable Long id) {
        salesOrderService.delete(id);
    }

    // ===== Quản lý dòng xe trong đơn =====

    @GetMapping("/{orderId}/items")
    public List<OrderItemResponse> getItems(@PathVariable Long orderId) {
        return salesOrderService.getItems(orderId).stream()
                .map(salesOrderMapper::toItemResponse)
                .toList();
    }

    @PostMapping("/{orderId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderItemResponse addItem(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderItemRequest request
    ) {
        return salesOrderMapper.toItemResponse(salesOrderService.addItem(orderId, request));
    }

    @PutMapping("/{orderId}/items/{itemId}")
    public OrderItemResponse updateItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody OrderItemRequest request
    ) {
        return salesOrderMapper.toItemResponse(salesOrderService.updateItem(orderId, itemId, request));
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable Long orderId, @PathVariable Long itemId) {
        salesOrderService.deleteItem(orderId, itemId);
    }

    private List<SalesOrderResponse> toResponses(List<SalesOrder> orders) {
        return orders.stream().map(salesOrderMapper::toResponse).toList();
    }
}
