package com.example.lab5.service.Impl;

import com.example.lab5.entity.Car;
import com.example.lab5.entity.CarCondition;
import com.example.lab5.entity.CarItem;
import com.example.lab5.entity.CarStatus;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.CarItemRepository;
import com.example.lab5.repository.CarRepository;
import com.example.lab5.repository.OrderItemRepository;
import com.example.lab5.repository.TestDriveAppointmentRepository;
import com.example.lab5.service.CarCatalogService;
import com.example.lab5.service.CarItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CarItemServiceImpl implements CarItemService {

    private final CarItemRepository carItemRepository;
    private final CarRepository carRepository;
    private final OrderItemRepository orderItemRepository;
    private final TestDriveAppointmentRepository testDriveAppointmentRepository;
    private final CarCatalogService carCatalogService;

    public CarItemServiceImpl(
            CarItemRepository carItemRepository,
            CarRepository carRepository,
            OrderItemRepository orderItemRepository,
            TestDriveAppointmentRepository testDriveAppointmentRepository,
            CarCatalogService carCatalogService
    ) {
        this.carItemRepository = carItemRepository;
        this.carRepository = carRepository;
        this.orderItemRepository = orderItemRepository;
        this.testDriveAppointmentRepository = testDriveAppointmentRepository;
        this.carCatalogService = carCatalogService;
    }

    @Override
    public List<CarItem> getByCar(Long carId) {
        requireCar(carId);
        return carItemRepository.findByCar_IdOrderByIdAsc(carId);
    }

    @Override
    public List<CarItem> getAvailableByCar(Long carId) {
        requireCar(carId);
        return carItemRepository.findByCar_IdAndStatusOrderByIdAsc(carId, CarStatus.AVAILABLE);
    }

    @Override
    public CarItem getById(Long id) {
        return carItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe có id = " + id));
    }

    @Override
    @Transactional
    public CarItem create(Long carId, CarItem item) {
        Car car = requireCar(carId);
        validate(item, car, null);

        item.setId(null);
        item.setCar(car);
        if (item.getCondition() == null) {
            item.setCondition(CarCondition.NEW);
        }
        if (item.getStatus() == null) {
            item.setStatus(CarStatus.AVAILABLE);
        }
        // Bỏ trống giá -> lấy giá niêm yết của dòng xe
        if (item.getPrice() == null) {
            item.setPrice(car.getBasePrice());
        }

        CarItem saved = carItemRepository.save(item);
        carCatalogService.evictCatalog();
        return saved;
    }

    @Override
    @Transactional
    public CarItem update(Long id, CarItem data) {
        CarItem existing = getById(id);
        Car car = existing.getCar();
        validate(data, car, id);

        existing.setVin(normalize(data.getVin()));
        existing.setColor(normalize(data.getColor()));
        existing.setYear(data.getYear());
        existing.setCondition(data.getCondition() == null ? CarCondition.NEW : data.getCondition());
        existing.setPrice(data.getPrice() == null ? car.getBasePrice() : data.getPrice());
        if (data.getStatus() != null && data.getStatus() != existing.getStatus()) {
            CarStatus requestedStatus = data.getStatus();
            CarStatus currentStatus = existing.getStatus();

            boolean validMaintenanceTransition =
                    (currentStatus == CarStatus.AVAILABLE
                            && requestedStatus == CarStatus.MAINTENANCE)
                    ||
                    (currentStatus == CarStatus.MAINTENANCE
                            && requestedStatus == CarStatus.AVAILABLE);

            if (!validMaintenanceTransition) {
                throw new BadRequestException(
                        "Chỉ được chuyển xe giữa trạng thái Sẵn sàng và Bảo trì"
                );
            }

            existing.setStatus(requestedStatus);
        }
        existing.setMileageKm(data.getMileageKm());
        existing.setNote(normalize(data.getNote()));

        CarItem saved = carItemRepository.save(existing);
        carCatalogService.evictCatalog();
        return saved;
    }

    @Override
    @Transactional
    public CarItem updateStatus(Long id, CarStatus status) {
        if (status == null) {
            throw new BadRequestException("Trạng thái không được để trống");
        }
        CarItem existing = getById(id);
        boolean allowedMaintenanceChange =
                (existing.getStatus() == CarStatus.AVAILABLE && status == CarStatus.MAINTENANCE)
                || (existing.getStatus() == CarStatus.MAINTENANCE && status == CarStatus.AVAILABLE);
        if (!allowedMaintenanceChange && existing.getStatus() != status) {
            throw new BadRequestException("Xe RESERVED/SOLD chỉ được thay đổi trạng thái thông qua luồng đơn hàng và hợp đồng");
        }
        existing.setStatus(status);
        CarItem saved = carItemRepository.save(existing);
        carCatalogService.evictCatalog();
        return saved;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CarItem item = getById(id);
        if (orderItemRepository.existsByCarItem_Id(id)
                || testDriveAppointmentRepository.existsByCarItem_Id(id)) {
            throw new BadRequestException("Không thể xoá xe vì đã phát sinh giao dịch");
        }
        carItemRepository.delete(item);
        carCatalogService.evictCatalog();
    }

    private Car requireCar(Long carId) {
        return carRepository.findById(carId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy dòng xe có id = " + carId));
    }

    private void validate(CarItem item, Car car, Long currentId) {
        if (item == null) {
            throw new BadRequestException("Dữ liệu xe không được để trống");
        }
        if (item.getPrice() != null && item.getPrice() < 0) {
            throw new BadRequestException("Giá bán không hợp lệ");
        }
        if (item.getPrice() == null && car.getBasePrice() == null) {
            throw new BadRequestException("Giá bán không được để trống");
        }
        if (item.getVin() != null && !item.getVin().isBlank()) {
            String vin = item.getVin().trim();
            carItemRepository.findByVin(vin).ifPresent(other -> {
                if (currentId == null || !other.getId().equals(currentId)) {
                    throw new BadRequestException("Số VIN đã tồn tại: " + vin);
                }
            });
        }
        if (item.getMileageKm() != null && item.getMileageKm() < 0) {
            throw new BadRequestException("Số km không hợp lệ");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
