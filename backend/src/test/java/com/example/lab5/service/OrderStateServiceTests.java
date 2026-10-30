package com.example.lab5.service;

import com.example.lab5.dto.OrderStatusHistoryResponse;
import com.example.lab5.entity.Customer;
import com.example.lab5.entity.OrderStatus;
import com.example.lab5.entity.Role;
import com.example.lab5.entity.SalesOrder;
import com.example.lab5.entity.User;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ForbiddenException;
import com.example.lab5.repository.OrderStatusHistoryRepository;
import com.example.lab5.repository.SalesOrderRepository;
import com.example.lab5.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderStateServiceTests {

    @Autowired
    private OrderStateService orderStateService;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private OrderStatusHistoryRepository historyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private SalesOrder order;
    private User owner;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(user("order-state-owner"));

        Customer customer = new Customer();
        customer.setFullName("Trần Thị Chủ Đơn");
        customer.setEmail("order-state-owner@example.com");
        customer.setUser(owner);
        entityManager.persist(customer);

        order = new SalesOrder();
        order.setOrderCode("ORD-STATE-001");
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(750_000_000d);
        order.setCustomer(customer);
        salesOrderRepository.save(order);

        orderStateService.recordInitial(order, owner.getUsername());
    }

    @Test
    void recordsInitialStatusAndEveryValidTransition() {
        orderStateService.transition(order, OrderStatus.CONFIRMED, "employee", "Đã gọi xác nhận");
        orderStateService.transition(order, OrderStatus.COMPLETED, "employee", "Đã giao xe");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        List<OrderStatusHistoryResponse> history =
                orderStateService.getHistory(order.getId(), owner.getUsername(), false);
        assertThat(history).hasSize(3);
        assertThat(history.get(0).fromStatus()).isNull();
        assertThat(history.get(0).toStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(history.get(1).fromStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(history.get(1).toStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(history.get(2).toStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(history.get(2).changedBy()).isEqualTo("employee");
    }

    @Test
    void rejectsInvalidTransitionAndKeepsHistoryUnchanged() {
        orderStateService.transition(order, OrderStatus.CONFIRMED, "employee", null);

        assertThatThrownBy(() ->
                orderStateService.transition(order, OrderStatus.PENDING, "employee", "Quay lại"))
                .isInstanceOf(BadRequestException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(historyRepository.findByOrder_IdOrderByChangedAtAscIdAsc(order.getId())).hasSize(2);
    }

    @Test
    void exposesAllowedTransitionsAndEnforcesOwnership() {
        assertThat(orderStateService.allowedNext(order.getId(), owner.getUsername(), false))
                .containsExactlyInAnyOrder(OrderStatus.CONFIRMED, OrderStatus.COMPLETED, OrderStatus.CANCELLED);

        assertThatThrownBy(() -> orderStateService.getHistory(order.getId(), "someone-else", false))
                .isInstanceOf(ForbiddenException.class);
        assertThat(orderStateService.getHistory(order.getId(), "admin", true)).hasSize(1);
    }

    private User user(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("irrelevant");
        user.setRole(Role.USER);
        user.setEnabled(true);
        return user;
    }
}
