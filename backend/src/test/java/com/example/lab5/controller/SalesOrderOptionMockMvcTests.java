package com.example.lab5.controller;

import com.example.lab5.entity.Customer;
import com.example.lab5.entity.Invoice;
import com.example.lab5.entity.OrderStatus;
import com.example.lab5.entity.Role;
import com.example.lab5.entity.SalesOrder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử API danh sách đơn hàng đủ điều kiện gắn chứng từ: /api/orders/options.
 */
@Transactional
class SalesOrderOptionMockMvcTests extends AbstractAdminMockMvcTest {

    @Autowired
    private EntityManager entityManager;

    private Long invoicedOrderId;
    private Long freeOrderId;

    @BeforeEach
    void setUpData() {
        ensureUser("user", "user", Role.USER);
        ensureUser("employee", "employee", Role.EMPLOYEE);

        Customer customer = new Customer();
        customer.setFullName("Nguyễn Văn A");
        customer.setPhone("0900000000");
        entityManager.persist(customer);

        SalesOrder invoiced = persistOrder(customer, "ORD-TEST-0001", OrderStatus.COMPLETED);
        SalesOrder free = persistOrder(customer, "ORD-TEST-0002", OrderStatus.CONFIRMED);

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV-TEST-0001");
        invoice.setTotal(100.0);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setOrder(invoiced);
        entityManager.persist(invoice);

        entityManager.flush();
        invoicedOrderId = invoiced.getId();
        freeOrderId = free.getId();
    }

    @Test
    void withoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/orders/options").param("target", "invoice"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void withUserTokenReturnsForbidden() throws Exception {
        String userToken = tokenFor("user", "user");

        mockMvc.perform(get("/api/orders/options").param("target", "invoice")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void withEmployeeTokenReturnsOnlyEligibleOrders() throws Exception {
        String employeeToken = tokenFor("employee", "employee");

        mockMvc.perform(get("/api/orders/options").param("target", "invoice")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].orderId", hasItem(freeOrderId.intValue())))
                .andExpect(jsonPath("$[*].orderId", not(hasItem(invoicedOrderId.intValue()))))
                .andExpect(jsonPath("$[0].orderCode").exists());
    }

    @Test
    void withUnknownTargetReturnsBadRequest() throws Exception {
        mockMvc.perform(asAdmin(get("/api/orders/options").param("target", "bogus")))
                .andExpect(status().isBadRequest());
    }

    private SalesOrder persistOrder(Customer customer, String code, OrderStatus status) {
        SalesOrder order = new SalesOrder();
        order.setOrderCode(code);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(status);
        order.setTotalAmount(100.0);
        order.setCustomer(customer);
        entityManager.persist(order);
        return order;
    }

    private String tokenFor(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return extractToken(body);
    }
}
