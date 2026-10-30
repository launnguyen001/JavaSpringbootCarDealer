package com.example.lab5.controller;

import com.example.lab5.entity.Brand;
import com.example.lab5.entity.Car;
import com.example.lab5.entity.CarCondition;
import com.example.lab5.entity.CarItem;
import com.example.lab5.entity.CarModel;
import com.example.lab5.entity.CarStatus;
import com.example.lab5.entity.Customer;
import com.example.lab5.entity.OrderItem;
import com.example.lab5.entity.OrderStatus;
import com.example.lab5.entity.SalesOrder;
import jakarta.persistence.EntityManager;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử các API dashboard & drilldown của ReportController (yêu cầu quyền ADMIN).
 */
@Transactional
class ReportControllerMockMvcTests extends AbstractAdminMockMvcTest {

    @Autowired
    private EntityManager entityManager;

    private Long brandId;
    private Long modelId;
    private Long carId;

    @BeforeEach
    void setUpData() {
        Brand brand = new Brand();
        brand.setName("Toyota");
        entityManager.persist(brand);

        CarModel model = new CarModel();
        model.setName("Camry");
        model.setBodyType("Sedan");
        model.setBrand(brand);
        entityManager.persist(model);

        Car car = new Car();
        car.setName("Camry 2.5");
        car.setBasePrice(1000.0);
        car.setModel(model);
        entityManager.persist(car);

        CarItem carItem = new CarItem();
        carItem.setCar(car);
        carItem.setVin("VIN-DASH-0001");
        carItem.setYear(2020);
        carItem.setColor("Black");
        carItem.setCondition(CarCondition.NEW);
        carItem.setPrice(1000.0);
        carItem.setStatus(CarStatus.AVAILABLE);
        entityManager.persist(carItem);

        Customer customer = new Customer();
        customer.setFullName("Nguyễn Văn A");
        customer.setPhone("0900000001");
        entityManager.persist(customer);

        SalesOrder order = new SalesOrder();
        order.setOrderCode("ORD-DASH-0001");
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(1000.0);
        order.setCustomer(customer);
        entityManager.persist(order);

        OrderItem item = new OrderItem();
        item.setQuantity(1);
        item.setUnitPrice(1000.0);
        item.setOrder(order);
        item.setCarItem(carItem);
        entityManager.persist(item);

        entityManager.flush();
        entityManager.clear();

        brandId = brand.getId();
        modelId = model.getId();
        carId = car.getId();
    }

    @Test
    void dashboardSummaryReturnsOk() throws Exception {
        mockMvc.perform(asAdmin(get("/api/reports/dashboard-summary")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCars", Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.availableCars", Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalCustomers", Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalOrders", Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalRevenue", Matchers.greaterThanOrEqualTo(1000.0)));
    }

    @Test
    void modelsByBrandReturnsModels() throws Exception {
        mockMvc.perform(asAdmin(get("/api/reports/inventory-by-brand/{brandId}/models", brandId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].modelName").value("Camry"))
                .andExpect(jsonPath("$[0].totalCars", Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void modelsByUnknownBrandReturnsNotFound() throws Exception {
        mockMvc.perform(asAdmin(get("/api/reports/inventory-by-brand/{brandId}/models", 9999999L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void carsByModelReturnsCars() throws Exception {
        mockMvc.perform(asAdmin(get("/api/reports/inventory-by-model/{modelId}/cars", modelId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].carName").value("Camry 2.5"))
                .andExpect(jsonPath("$[0].vin").value("VIN-DASH-0001"))
                .andExpect(jsonPath("$[0].year").value(2020));
    }

    @Test
    void ordersByCarReturnsOrderLines() throws Exception {
        mockMvc.perform(asAdmin(get("/api/reports/top-selling-cars/{carId}/orders", carId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderCode").value("ORD-DASH-0001"))
                .andExpect(jsonPath("$[0].quantity").value(1));
    }

    @Test
    void ordersByStatusReturnsOk() throws Exception {
        mockMvc.perform(asAdmin(get("/api/reports/orders-by-status/{status}", "PENDING")))
                .andExpect(status().isOk());
    }

    @Test
    void ordersByInvalidStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(asAdmin(get("/api/reports/orders-by-status/{status}", "INVALID")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void revenueByMonthReturnsOk() throws Exception {
        mockMvc.perform(asAdmin(get("/api/reports/revenue-by-month")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].period", Matchers.notNullValue()));
    }

    @Test
    void reportsWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/reports/dashboard-summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exportDashboardSummaryAsXlsxReturnsExcelAttachment() throws Exception {
        mockMvc.perform(asAdmin(get("/api/reports/export")
                        .param("type", "dashboard-summary")
                        .param("format", "xlsx")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
                .andExpect(header().string("Content-Disposition",
                        Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition",
                        Matchers.containsString("bao-cao-dashboard-summary-")));
    }

    @Test
    void exportRevenueSummaryAsCsvReturnsUtf8Csv() throws Exception {
        mockMvc.perform(asAdmin(get("/api/reports/export")
                        .param("type", "revenue-summary")
                        .param("format", "csv")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.parseMediaType("text/csv;charset=UTF-8")))
                .andExpect(header().string("Content-Disposition",
                        Matchers.containsString("bao-cao-revenue-summary-")))
                .andExpect(content().string(Matchers.containsString("Tổng doanh thu")));
    }

    @Test
    void exportInvalidTypeReturnsBadRequest() throws Exception {
        mockMvc.perform(asAdmin(get("/api/reports/export")
                        .param("type", "unknown")
                        .param("format", "xlsx")))
                .andExpect(status().isBadRequest());
    }
}
