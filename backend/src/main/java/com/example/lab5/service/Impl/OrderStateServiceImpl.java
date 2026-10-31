package com.example.lab5.service.Impl;

import com.example.lab5.dto.OrderStatusHistoryResponse;
import com.example.lab5.entity.OrderStatus;
import com.example.lab5.entity.OrderStatusHistory;
import com.example.lab5.entity.SalesOrder;
import com.example.lab5.event.NotificationEventFactory;
import com.example.lab5.exception.ForbiddenException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.mapper.OrderStatusHistoryMapper;
import com.example.lab5.repository.OrderStatusHistoryRepository;
import com.example.lab5.repository.SalesOrderRepository;
import com.example.lab5.service.OrderStateService;
import com.example.lab5.statemachine.OrderStateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderStateServiceImpl implements OrderStateService {

    private final OrderStateMachine stateMachine;
    private final SalesOrderRepository salesOrderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OrderStatusHistoryMapper historyMapper;
    private final NotificationEventFactory notificationEvents;

    public OrderStateServiceImpl(
            OrderStateMachine stateMachine,
            SalesOrderRepository salesOrderRepository,
            OrderStatusHistoryRepository historyRepository,
            OrderStatusHistoryMapper historyMapper,
            NotificationEventFactory notificationEvents
    ) {
        this.stateMachine = stateMachine;
        this.salesOrderRepository = salesOrderRepository;
        this.historyRepository = historyRepository;
        this.historyMapper = historyMapper;
        this.notificationEvents = notificationEvents;
    }

    @Override
    @Transactional
    public void recordInitial(SalesOrder order, String actor) {
        saveHistory(order, null, order.getStatus(), actor, "Tạo đơn hàng");
    }

    @Override
    @Transactional
    public SalesOrder transition(SalesOrder order, OrderStatus target, String actor, String note) {
        OrderStatus current = order.getStatus();
        if (current == target) {
            return order;
        }
        stateMachine.assertCanTransition(current, target);
        order.setStatus(target);
        if (target == OrderStatus.CONFIRMED && order.getConfirmedAt() == null) {
            order.setConfirmedAt(LocalDateTime.now());
        }
        SalesOrder saved = salesOrderRepository.save(order);
        saveHistory(saved, current, target, actor, note);
        notificationEvents.orderStatusChanged(saved, current, target);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getHistory(Long orderId, String username, boolean staff) {
        SalesOrder order = loadOrderForUser(orderId, username, staff);
        return historyRepository.findByOrder_IdOrderByChangedAtAscIdAsc(order.getId()).stream()
                .map(historyMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<OrderStatus> allowedNext(Long orderId, String username, boolean staff) {
        SalesOrder order = loadOrderForUser(orderId, username, staff);
        return stateMachine.allowedNext(order.getStatus());
    }

    @Override
    public Map<OrderStatus, Set<OrderStatus>> transitions() {
        return stateMachine.transitions();
    }

    private SalesOrder loadOrderForUser(Long orderId, String username, boolean staff) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng có id = " + orderId));
        if (staff) {
            return order;
        }
        boolean owner = order.getCustomer() != null
                && order.getCustomer().getUser() != null
                && username != null
                && username.equals(order.getCustomer().getUser().getUsername());
        if (!owner) {
            throw new ForbiddenException("Bạn không có quyền xem đơn hàng này");
        }
        return order;
    }

    private void saveHistory(SalesOrder order, OrderStatus from, OrderStatus to, String actor, String note) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setChangedBy(actor == null || actor.isBlank() ? "system" : actor);
        history.setNote(note);
        history.setChangedAt(LocalDateTime.now());
        historyRepository.save(history);
    }
}
