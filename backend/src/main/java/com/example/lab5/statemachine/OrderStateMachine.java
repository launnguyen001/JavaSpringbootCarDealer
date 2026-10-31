package com.example.lab5.statemachine;

import com.example.lab5.entity.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * Luồng trạng thái đơn hàng:
 *
 * <pre>
 * PENDING ──► CONFIRMED ──► COMPLETED
 *    │            │
 *    └────────────┴──────► CANCELLED
 * </pre>
 *
 * <p>PENDING chỉ được xác nhận hoặc hủy. Chỉ đơn CONFIRMED mới có thể hoàn tất sau khi thu đủ tiền.
 */
@Component
public class OrderStateMachine extends StateMachine<OrderStatus> {

    public OrderStateMachine() {
        super(OrderStatus.class, "đơn hàng", definition());
    }

    private static Map<OrderStatus, EnumSet<OrderStatus>> definition() {
        Map<OrderStatus, EnumSet<OrderStatus>> map = new EnumMap<>(OrderStatus.class);
        map.put(OrderStatus.PENDING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        map.put(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED));
        map.put(OrderStatus.COMPLETED, EnumSet.noneOf(OrderStatus.class));
        map.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        return map;
    }
}
