package com.example.lab5.controller;

import com.example.lab5.dto.SalesOrderRequest;
import com.example.lab5.dto.SalesOrderResponse;
import com.example.lab5.entity.OrderStatus;
import com.example.lab5.entity.SalesOrder;
import com.example.lab5.mapper.SalesOrderMapper;
import com.example.lab5.service.SalesOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    private List<SalesOrderResponse> toResponses(List<SalesOrder> orders) {
        return orders.stream().map(salesOrderMapper::toResponse).toList();
    }
}
