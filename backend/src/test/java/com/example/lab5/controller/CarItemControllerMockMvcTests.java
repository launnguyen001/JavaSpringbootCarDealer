package com.example.lab5.controller;

import com.example.lab5.entity.Brand;
import com.example.lab5.entity.Car;
import com.example.lab5.entity.CarCondition;
import com.example.lab5.entity.CarItem;
import com.example.lab5.entity.CarModel;
import com.example.lab5.entity.CarStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử API từng chiếc xe vật lý (/api/car-items).
 * Đọc công khai, ghi cần quyền ADMIN.
 */
@Transactional
class CarItemControllerMockMvcTests extends AbstractAdminMockMvcTest {

    @Autowired
    private EntityManager entityManager;

    private Long carId;
    private Long itemId;
    private Long modelId;

    @BeforeEach
    void setUp() {
        Brand brand = new Brand();
        brand.setName("Toyota");
        entityManager.persist(brand);

        CarModel model = new CarModel();
        model.setName("Camry");
        model.setBrand(brand);
        entityManager.persist(model);
        modelId = model.getId();

        Car car = new Car();
        car.setName("Camry 2.0");
        car.setBasePrice(100.0);
        car.setModel(model);
        entityManager.persist(car);

        CarItem item = new CarItem();
        item.setCar(car);
        item.setVin("VIN-TEST-0001");
        item.setYear(2024);
        item.setColor("Đen");
        item.setCondition(CarCondition.NEW);
        item.setPrice(100.0);
        item.setStatus(CarStatus.AVAILABLE);
        entityManager.persist(item);

        entityManager.flush();
        carId = car.getId();
        itemId = item.getId();
    }

    @Test
    void listItemsByCarIsPublic() throws Exception {
        mockMvc.perform(get("/api/car-items").param("carId", String.valueOf(carId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vin").value("VIN-TEST-0001"))
                .andExpect(jsonPath("$[0].carName").value("Camry 2.0"))
                .andExpect(jsonPath("$[0].condition").value("NEW"));
    }

    @Test
    void getItemByIdIsPublic() throws Exception {
        mockMvc.perform(get("/api/car-items/{id}", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId))
                .andExpect(jsonPath("$.price").value(100.0));
    }

    @Test
    void createItemRequiresAdmin() throws Exception {
        String json = """
                {"vin": "VIN-NEW-0002", "price": 90.0, "condition": "USED", "mileageKm": 15000}
                """;

        mockMvc.perform(post("/api/car-items")
                        .param("carId", String.valueOf(carId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(asAdmin(post("/api/car-items")
                        .param("carId", String.valueOf(carId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vin").value("VIN-NEW-0002"))
                .andExpect(jsonPath("$.condition").value("USED"));
    }

    @Test
    void createItemRejectsDuplicateVin() throws Exception {
        String json = """
                {"vin": "VIN-TEST-0001", "price": 80.0}
                """;

        mockMvc.perform(asAdmin(post("/api/car-items")
                        .param("carId", String.valueOf(carId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateItemChangesFields() throws Exception {
        String json = """
                {"vin": "VIN-TEST-0001", "price": 95.0, "condition": "USED", "mileageKm": 20000, "note": "Xe cũ"}
                """;

        mockMvc.perform(asAdmin(put("/api/car-items/{id}", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(95.0))
                .andExpect(jsonPath("$.condition").value("USED"))
                .andExpect(jsonPath("$.mileageKm").value(20000));
    }

    @Test
    void patchStatusUpdatesItemStatus() throws Exception {
        mockMvc.perform(asAdmin(patch("/api/car-items/{id}/status", itemId)
                        .param("status", "MAINTENANCE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MAINTENANCE"));
    }

    @Test
    void deleteItemReturnsNoContent() throws Exception {
        mockMvc.perform(asAdmin(delete("/api/car-items/{id}", itemId)))
                .andExpect(status().isNoContent());
    }

    @Test
    void getItemReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/car-items/{id}", 9999999L))
                .andExpect(status().isNotFound());
    }
}
