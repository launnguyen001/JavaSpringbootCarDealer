package com.example.lab5.config;

import com.example.lab5.entity.Brand;
import com.example.lab5.entity.Car;
import com.example.lab5.entity.CarModel;
import com.example.lab5.entity.Customer;
import com.example.lab5.entity.Employee;
import com.example.lab5.repository.BrandRepository;
import com.example.lab5.repository.CarModelRepository;
import com.example.lab5.repository.CarRepository;
import com.example.lab5.repository.CustomerRepository;
import com.example.lab5.repository.EmployeeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// Seed dữ liệu mẫu cho môi trường chạy thật (không chạy khi test)
@Component
@Profile("!test")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final BrandRepository brandRepository;
    private final CarModelRepository carModelRepository;
    private final CarRepository carRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;

    public DataSeeder(
            BrandRepository brandRepository,
            CarModelRepository carModelRepository,
            CarRepository carRepository,
            CustomerRepository customerRepository,
            EmployeeRepository employeeRepository
    ) {
        this.brandRepository = brandRepository;
        this.carModelRepository = carModelRepository;
        this.carRepository = carRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public void run(String... args) {
        if (!brandRepository.findAll().isEmpty()) {
            return;
        }

        Brand toyota = brandRepository.save(brand("Toyota", "Nhật Bản"));
        Brand honda = brandRepository.save(brand("Honda", "Nhật Bản"));
        Brand ford = brandRepository.save(brand("Ford", "Hoa Kỳ"));

        CarModel camry = carModelRepository.save(model("Camry", "Sedan", toyota));
        CarModel corolla = carModelRepository.save(model("Corolla", "Sedan", toyota));
        CarModel civic = carModelRepository.save(model("Civic", "Sedan", honda));
        CarModel ranger = carModelRepository.save(model("Ranger", "Bán tải", ford));

        carRepository.save(car("Camry 2.0", "CAM-001", 950.0, 5, 2024, "Đen", camry));
        carRepository.save(car("Camry 2.5", "CAM-002", 1200.0, 3, 2024, "Trắng", camry));
        carRepository.save(car("Corolla Cross", "COR-001", 800.0, 8, 2023, "Bạc", corolla));
        carRepository.save(car("Civic 1.5", "CIV-001", 780.0, 6, 2024, "Xanh", civic));
        carRepository.save(car("Ranger Wildtrak", "RAN-001", 1100.0, 4, 2023, "Cam", ranger));

        customerRepository.save(customer("Nguyễn Văn A", "a@example.com", "0900000001"));
        customerRepository.save(customer("Trần Thị B", "b@example.com", "0900000002"));

        employeeRepository.save(employee("Lê Văn C", "c@cardealer.vn", "Sales", "0911000001"));
        employeeRepository.save(employee("Phạm Thị D", "d@cardealer.vn", "Manager", "0911000002"));
    }

    private Brand brand(String name, String country) {
        Brand brand = new Brand();
        brand.setName(name);
        brand.setCountry(country);
        return brand;
    }

    private CarModel model(String name, String bodyType, Brand brand) {
        CarModel model = new CarModel();
        model.setName(name);
        model.setBodyType(bodyType);
        model.setBrand(brand);
        return model;
    }

    private Car car(String name, String vin, Double price, Integer quantity,
                    Integer year, String color, CarModel model) {
        Car car = new Car();
        car.setName(name);
        car.setVin(vin);
        car.setPrice(price);
        car.setQuantity(quantity);
        car.setYear(year);
        car.setColor(color);
        car.setModel(model);
        return car;
    }

    private Customer customer(String fullName, String email, String phone) {
        Customer customer = new Customer();
        customer.setFullName(fullName);
        customer.setEmail(email);
        customer.setPhone(phone);
        return customer;
    }

    private Employee employee(String fullName, String email, String phone, String position) {
        Employee employee = new Employee();
        employee.setFullName(fullName);
        employee.setEmail(email);
        employee.setPhone(phone);
        employee.setPosition(position);
        return employee;
    }
}
