package com.example.lab5.service;

import com.example.lab5.dto.OrderStatusHistoryResponse;
import com.example.lab5.entity.OrderStatus;
import com.example.lab5.entity.SalesOrder;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Trung tâm xử lý chuyển trạng thái đơn hàng: validate state machine + ghi lịch sử + phát thông báo. */
public interface OrderStateService {

    /** Ghi mốc trạng thái đầu tiên khi tạo đơn (fromStatus = null). */
    void recordInitial(SalesOrder order, String actor);

    /** Chuyển trạng thái hợp lệ, lưu đơn, ghi lịch sử và phát thông báo. */
    SalesOrder transition(SalesOrder order, OrderStatus target, String actor, String note);

    List<OrderStatusHistoryResponse> getHistory(Long orderId, String username, boolean staff);

    Set<OrderStatus> allowedNext(Long orderId, String username, boolean staff);

    Map<OrderStatus, Set<OrderStatus>> transitions();
}
