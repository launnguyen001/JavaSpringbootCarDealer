package com.example.lab5.mapper;

import com.example.lab5.dto.OrderStatusHistoryResponse;
import com.example.lab5.entity.OrderStatusHistory;
import org.springframework.stereotype.Component;

@Component
public class OrderStatusHistoryMapper {

    public OrderStatusHistoryResponse toResponse(OrderStatusHistory history) {
        return new OrderStatusHistoryResponse(
                history.getId(),
                history.getOrder() == null ? null : history.getOrder().getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy(),
                history.getNote(),
                history.getChangedAt()
        );
    }
}
