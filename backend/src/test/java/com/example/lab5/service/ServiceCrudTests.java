package com.example.lab5.service;

import com.example.lab5.entity.Brand;
import com.example.lab5.entity.Car;
import com.example.lab5.entity.CarCondition;
import com.example.lab5.entity.CarItem;
import com.example.lab5.entity.CarModel;
import com.example.lab5.entity.CarStatus;
import com.example.lab5.entity.Customer;
import com.example.lab5.entity.Employee;
import com.example.lab5.entity.SalesOrder;
import com.example.lab5.entity.TestDriveAppointment;
import com.example.lab5.exception.BadRequestException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ServiceCrudTests {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CarService carService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void customerUpdateChangesStoredFields() {
        Customer customer = persistCustomer("Nguyễn Văn A");

        Customer form = new Customer();
        form.setFullName("Trần Thị B");
        form.setEmail("b@example.com");
        form.setPhone("0987654321");
        form.setAddress("Đà Nẵng");
        form.setNationalId("999999999");

        Customer updated = customerService.update(customer.getId(), form);

        assertThat(updated.getId()).isEqualTo(customer.getId());
        assertThat(updated.getFullName()).isEqualTo("Trần Thị B");
        assertThat(updated.getEmail()).isEqualTo("b@example.com");
        assertThat(updated.getAddress()).isEqualTo("Đà Nẵng");
    }

    @Test
    void carUpdateChangesStoredFields() {
        Car car = persistCarWithItems(5);

        Car form = new Car();
        form.setName("Camry 2.5");
        form.setDescription("Bản nâng cấp");
        form.setBasePrice(250.0);
        CarModel reference = new CarModel();
        reference.setId(car.getModel().getId());
        form.setModel(reference);

        Car updated = carService.update(car.getId(), form);

        assertThat(updated.getId()).isEqualTo(car.getId());
        assertThat(updated.getName()).isEqualTo("Camry 2.5");
        assertThat(updated.getBasePrice()).isEqualTo(250.0);
        assertThat(updated.getDescription()).isEqualTo("Bản nâng cấp");
    }

    @Test
    void deleteCustomerBlockedWhenHasOrder() {
        Customer customer = persistCustomer("Khách có đơn");

        SalesOrder order = new SalesOrder();
        order.setOrderCode("ORD-" + System.nanoTime());
        order.setCustomer(customer);
        entityManager.persist(order);
        entityManager.flush();

        assertThatThrownBy(() -> customerService.delete(customer.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void deleteEmployeeBlockedWhenResponsibleForOrder() {
        Employee employee = new Employee();
        employee.setFullName("Lê Văn C");
        entityManager.persist(employee);

        Customer customer = persistCustomer("Khách của nhân viên");
        SalesOrder order = new SalesOrder();
        order.setOrderCode("ORD-" + System.nanoTime());
        order.setCustomer(customer);
        order.setEmployee(employee);
        entityManager.persist(order);
        entityManager.flush();

        assertThatThrownBy(() -> employeeService.delete(employee.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void deleteCarBlockedWhenHasTestDrive() {
        Car car = persistCarWithItems(3);
        CarItem carItem = firstItemOf(car.getId());

        TestDriveAppointment appointment = new TestDriveAppointment();
        appointment.setScheduledAt(LocalDateTime.now().plusDays(1));
        appointment.setCustomer(persistCustomer("Khách lái thử"));
        appointment.setCarItem(carItem);
        entityManager.persist(appointment);
        entityManager.flush();

        assertThatThrownBy(() -> carService.delete(car.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void deleteCustomerSucceedsWhenNoReferences() {
        Customer customer = persistCustomer("Khách không tham chiếu");
        Long id = customer.getId();

        customerService.delete(id);
        entityManager.flush();

        assertThat(entityManager.find(Customer.class, id)).isNull();
    }

    @Test
    void salesOrderDeleteBlockedWhenNotCancelled() {
        Customer customer = persistCustomer("Khách có đơn chưa huỷ");
        Car car = persistCarWithItems(5);
        CarItem carItem = firstItemOf(car.getId());
        SalesOrder order = salesOrderService.create(new com.example.lab5.dto.SalesOrderRequest(
                customer.getId(),
                null,
                java.util.List.of(new com.example.lab5.dto.OrderItemRequest(carItem.getId(), 1))
        ));

        assertThatThrownBy(() -> salesOrderService.delete(order.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    private Customer persistCustomer(String fullName) {
        Customer customer = new Customer();
        customer.setFullName(fullName + "-" + System.nanoTime());
        entityManager.persist(customer);
        entityManager.flush();
        return customer;
    }

    // Tạo một dòng xe kèm `itemCount` chiếc vật lý
    private Car persistCarWithItems(int itemCount) {
        Brand brand = new Brand();
        brand.setName("Toyota-" + System.nanoTime());
        entityManager.persist(brand);

        CarModel model = new CarModel();
        model.setName("Camry");
        model.setBrand(brand);
        entityManager.persist(model);

        Car car = new Car();
        car.setName("Camry 2.0");
        car.setBasePrice(100.0);
        car.setModel(model);
        entityManager.persist(car);

        for (int i = 1; i <= itemCount; i++) {
            CarItem item = new CarItem();
            item.setCar(car);
            item.setVin("VIN-" + System.nanoTime() + "-" + i);
            item.setCondition(CarCondition.NEW);
            item.setPrice(100.0);
            item.setStatus(CarStatus.AVAILABLE);
            entityManager.persist(item);
        }
        entityManager.flush();
        return car;
    }

    private CarItem firstItemOf(Long carId) {
        return entityManager
                .createQuery("SELECT i FROM CarItem i WHERE i.car.id = :carId ORDER BY i.id", CarItem.class)
                .setParameter("carId", carId)
                .setMaxResults(1)
                .getSingleResult();
    }
}
