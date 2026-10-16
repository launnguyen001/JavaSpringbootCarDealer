package com.example.lab5.controller;

import com.example.lab5.entity.Customer;
import com.example.lab5.entity.SalesOrder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class CustomerControllerMockMvcTests extends AbstractAdminMockMvcTest {

    @Autowired
    private EntityManager entityManager;

    private Long customerId;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer();
        customer.setFullName("Nguyễn Văn A");
        customer.setPhone("0900000000");
        entityManager.persist(customer);
        entityManager.flush();
        customerId = customer.getId();
    }

    @Test
    void updateCustomerReturnsUpdatedCustomer() throws Exception {
        String json = """
                {
                  "fullName": "Nguyễn Văn B",
                  "email": "b@example.com",
                  "phone": "0911111111",
                  "address": "Hà Nội",
                  "nationalId": "0123456789"
                }
                """;

        mockMvc.perform(asAdmin(put("/api/customers/{id}", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId))
                .andExpect(jsonPath("$.fullName").value("Nguyễn Văn B"))
                .andExpect(jsonPath("$.email").value("b@example.com"));
    }

    @Test
    void updateCustomerReturnsNotFound() throws Exception {
        String json = """
                {"fullName": "Không tồn tại"}
                """;

        mockMvc.perform(asAdmin(put("/api/customers/{id}", 9999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCustomerWithoutOrdersReturnsNoContent() throws Exception {
        mockMvc.perform(asAdmin(delete("/api/customers/{id}", customerId)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCustomerReturnsNotFound() throws Exception {
        mockMvc.perform(asAdmin(delete("/api/customers/{id}", 9999999L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCustomerBlockedWhenStillHasOrders() throws Exception {
        SalesOrder order = new SalesOrder();
        order.setOrderCode("ORD-TEST-" + System.nanoTime());
        order.setCustomer(entityManager.find(Customer.class, customerId));
        entityManager.persist(order);
        entityManager.flush();

        mockMvc.perform(asAdmin(delete("/api/customers/{id}", customerId)))
                .andExpect(status().isBadRequest());
    }
}
