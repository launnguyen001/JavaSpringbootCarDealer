package com.example.lab5.controller;

import com.example.lab5.entity.Brand;
import com.example.lab5.entity.CarModel;
import com.example.lab5.entity.Role;
import com.example.lab5.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử phân quyền RBAC: 401 khi thiếu token, 403 khi sai quyền, 2xx khi đúng quyền.
 */
@Transactional
class SecurityAuthorizationTests extends AbstractAdminMockMvcTest {

    @Autowired
    private EntityManager entityManager;

    private Long modelId;

    @BeforeEach
    void setUpData() {
        ensureUser("user", "user", Role.USER);
        ensureUser("employee", "employee", Role.EMPLOYEE);

        Brand brand = new Brand();
        brand.setName("Toyota");
        entityManager.persist(brand);

        CarModel model = new CarModel();
        model.setName("Camry");
        model.setBrand(brand);
        entityManager.persist(model);
        entityManager.flush();
        modelId = model.getId();
    }

    private String tokenFor(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return extractToken(body);
    }

    private String carJson() {
        return """
                {"name":"Civic","basePrice":200.0,"modelId":%d}
                """.formatted(modelId);
    }

    // ---------- Đọc catalog công khai ----------

    @Test
    void publicReadCarsWithoutTokenReturnsOk() throws Exception {
        mockMvc.perform(get("/api/cars"))
                .andExpect(status().isOk());
    }

    @Test
    void publicReadBrandsWithoutTokenReturnsOk() throws Exception {
        mockMvc.perform(get("/api/brands"))
                .andExpect(status().isOk());
    }

    // ---------- Ghi catalog: ADMIN ----------

    @Test
    void createCarWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCarWithUserTokenReturnsForbidden() throws Exception {
        String userToken = tokenFor("user", "user");

        mockMvc.perform(post("/api/cars")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCarWithEmployeeTokenReturnsForbidden() throws Exception {
        String employeeToken = tokenFor("employee", "employee");

        mockMvc.perform(post("/api/cars")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCarWithAdminTokenReturnsCreated() throws Exception {
        mockMvc.perform(asAdmin(post("/api/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carJson())))
                .andExpect(status().isCreated());
    }

    // ---------- Báo cáo: ADMIN ----------

    @Test
    void reportsWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/reports/orders-by-status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reportsWithUserTokenReturnsForbidden() throws Exception {
        String userToken = tokenFor("user", "user");

        mockMvc.perform(get("/api/reports/orders-by-status")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void reportsWithAdminTokenReturnsOk() throws Exception {
        mockMvc.perform(asAdmin(get("/api/reports/orders-by-status")))
                .andExpect(status().isOk());
    }

    // ---------- Khách hàng: staff đọc, user tạo được hồ sơ ----------

    @Test
    void listCustomersWithUserTokenReturnsForbidden() throws Exception {
        String userToken = tokenFor("user", "user");

        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void listCustomersWithEmployeeTokenReturnsOk() throws Exception {
        String employeeToken = tokenFor("employee", "employee");

        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());
    }

    @Test
    void createCustomerWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Nguyễn Văn A\",\"phone\":\"0900000001\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCustomerWithUserTokenReturnsCreated() throws Exception {
        String userToken = tokenFor("user", "user");

        mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Nguyễn Văn A\",\"phone\":\"0900000001\"}"))
                .andExpect(status().isCreated());
    }

    // ---------- Nhân viên: xem = staff, tạo = ADMIN ----------

    @Test
    void listEmployeesWithEmployeeTokenReturnsOk() throws Exception {
        String employeeToken = tokenFor("employee", "employee");

        mockMvc.perform(get("/api/employees")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());
    }

    @Test
    void createEmployeeWithEmployeeTokenReturnsForbidden() throws Exception {
        String employeeToken = tokenFor("employee", "employee");

        mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Lê Văn C\",\"position\":\"Sales\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpointWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }
}
