package com.example.lab5.controller;

import com.example.lab5.entity.Brand;
import com.example.lab5.entity.Car;
import com.example.lab5.entity.CarModel;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class CarControllerMockMvcTests extends AbstractAdminMockMvcTest {

    @Autowired
    private EntityManager entityManager;

    private Long carId;
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
        car.setPrice(100.0);
        car.setQuantity(5);
        car.setModel(model);
        entityManager.persist(car);
        entityManager.flush();
        carId = car.getId();
    }

    @Test
    void getAllCarsReturnsOk() throws Exception {
        mockMvc.perform(get("/api/cars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getCarByIdReturnsCar() throws Exception {
        mockMvc.perform(get("/api/cars/{id}", carId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Camry 2.0"))
                .andExpect(jsonPath("$.modelName").value("Camry"))
                .andExpect(jsonPath("$.brandName").value("Toyota"));
    }

    @Test
    void getCarByIdReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/cars/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchByNameReturnsMatchingCars() throws Exception {
        mockMvc.perform(get("/api/cars/search/name").param("name", "CAMRY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Camry 2.0"));
    }

    @Test
    void addCarReturnsCreated() throws Exception {
        String json = """
                {
                  "name": "Civic 1.5",
                  "price": 200.0,
                  "quantity": 3,
                  "modelId": %d
                }
                """.formatted(modelId);

        mockMvc.perform(asAdmin(post("/api/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Civic 1.5"))
                .andExpect(jsonPath("$.modelId").value(modelId));
    }

    @Test
    void updateCarReturnsUpdatedCar() throws Exception {
        String json = """
                {
                  "name": "Camry 2.5",
                  "price": 150.0,
                  "quantity": 2,
                  "color": "Trắng",
                  "modelId": %d
                }
                """.formatted(modelId);

        mockMvc.perform(asAdmin(put("/api/cars/{id}", carId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(carId))
                .andExpect(jsonPath("$.name").value("Camry 2.5"))
                .andExpect(jsonPath("$.price").value(150.0))
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void updateCarReturnsNotFound() throws Exception {
        String json = """
                {
                  "name": "Không tồn tại",
                  "price": 100.0,
                  "quantity": 1,
                  "modelId": %d
                }
                """.formatted(modelId);

        mockMvc.perform(asAdmin(put("/api/cars/{id}", 9999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCarReturnsNoContent() throws Exception {
        mockMvc.perform(asAdmin(delete("/api/cars/{id}", carId)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCarReturnsNotFound() throws Exception {
        mockMvc.perform(asAdmin(delete("/api/cars/{id}", 9999999L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void addCarReturnsBadRequestWhenNameMissing() throws Exception {
        String json = """
                {
                  "price": 200.0,
                  "quantity": 3,
                  "modelId": %d
                }
                """.formatted(modelId);

        mockMvc.perform(asAdmin(post("/api/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)))
                .andExpect(status().isBadRequest());
    }
}
