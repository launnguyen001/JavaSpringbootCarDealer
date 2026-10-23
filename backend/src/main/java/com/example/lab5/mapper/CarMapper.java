package com.example.lab5.mapper;

import com.example.lab5.dto.CarItemResponse;
import com.example.lab5.dto.CarRequest;
import com.example.lab5.dto.CarResponse;
import com.example.lab5.entity.Car;
import com.example.lab5.entity.CarCondition;
import com.example.lab5.entity.CarItem;
import com.example.lab5.entity.CarModel;
import com.example.lab5.entity.CarStatus;
import com.example.lab5.mongorepository.CarMediaRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class CarMapper {

    private final CarItemMapper carItemMapper;
    private final CarMediaRepository carMediaRepository;

    public CarMapper(CarItemMapper carItemMapper, CarMediaRepository carMediaRepository) {
        this.carItemMapper = carItemMapper;
        this.carMediaRepository = carMediaRepository;
    }

    public Car toEntity(CarRequest request) {
        if (request == null) {
            return null;
        }
        CarModel model = null;
        if (request.modelId() != null) {
            model = new CarModel();
            model.setId(request.modelId());
        }
        Car car = new Car();
        car.setName(request.name());
        car.setDescription(request.description());
        car.setBasePrice(request.basePrice());
        car.setModel(model);
        return car;
    }

    /** Dòng xe cho danh sách / tìm kiếm — kèm số liệu tổng hợp, KHÔNG kèm từng chiếc. */
    public CarResponse toResponse(Car car) {
        return build(car, false);
    }

    /** Dòng xe cho trang chi tiết — kèm danh sách từng chiếc (car_items). */
    public CarResponse toDetailResponse(Car car) {
        return build(car, true);
    }

    private CarResponse build(Car car, boolean includeItems) {
        if (car == null) {
            return null;
        }
        List<CarItem> items = car.getItems() == null ? List.of() : car.getItems();

        long available = 0;
        long reserved = 0;
        long sold = 0;
        long maintenance = 0;
        Double priceFrom = null;
        Double priceTo = null;
        Set<CarCondition> conditions = new LinkedHashSet<>();

        for (CarItem item : items) {
            if (item.getStatus() == CarStatus.RESERVED) {
                reserved++;
            } else if (item.getStatus() == CarStatus.SOLD) {
                sold++;
            } else if (item.getStatus() == CarStatus.MAINTENANCE) {
                maintenance++;
            } else {
                available++;
            }

            Double price = item.getPrice();
            if (price != null) {
                if (priceFrom == null || price < priceFrom) {
                    priceFrom = price;
                }
                if (priceTo == null || price > priceTo) {
                    priceTo = price;
                }
            }
            if (item.getCondition() != null) {
                conditions.add(item.getCondition());
            }
        }

        // Chưa có chiếc nào -> lấy giá niêm yết làm khoảng giá tham khảo
        if (priceFrom == null) {
            priceFrom = car.getBasePrice();
            priceTo = car.getBasePrice();
        }

        List<CarCondition> conditionList = new ArrayList<>(conditions);
        conditionList.sort(Comparator.comparingInt((CarCondition condition) -> condition.ordinal()));

        List<CarItemResponse> itemResponses = includeItems
                ? items.stream().map(item -> carItemMapper.toResponse(item, car)).toList()
                : List.of();

        Long modelId = null;
        String modelName = null;
        String brandName = null;
        if (car.getModel() != null) {
            modelId = car.getModel().getId();
            modelName = car.getModel().getName();
            if (car.getModel().getBrand() != null) {
                brandName = car.getModel().getBrand().getName();
            }
        }

        // Ảnh đại diện (thumbnail) từ car-media Mongo; nếu chưa có/không đọc được thì null (dùng fallback ở UI).
        String thumbnail = fetchThumbnail(car.getId());

        return new CarResponse(
                car.getId(),
                car.getName(),
                car.getDescription(),
                car.getBasePrice(),
                priceFrom,
                priceTo,
                items.size(),
                available,
                reserved,
                sold,
                maintenance,
                conditionList,
                deriveStatus(items.size(), available, reserved, maintenance, sold),
                modelId,
                modelName,
                brandName,
                thumbnail,
                itemResponses
        );
    }

    /**
     * Đọc thumbnail của xe từ MongoDB car-media. Bọc try-catch vì môi trường test chỉ tạo bean Mongo
     * mà chưa chắc đã kết nối được — trong trường hợp đó coi như không có media (trả null).
     */
    private String fetchThumbnail(Long carId) {
        try {
            return carMediaRepository.findByCarId(carId)
                    .map(media -> media.getThumbnail())
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    // Trạng thái hiển thị của dòng xe suy ra từ các chiếc thực tế
    private CarStatus deriveStatus(long total, long available, long reserved, long maintenance, long sold) {
        if (total == 0 || available > 0) {
            return CarStatus.AVAILABLE;
        }
        if (reserved > 0) {
            return CarStatus.RESERVED;
        }
        if (maintenance > 0) {
            return CarStatus.MAINTENANCE;
        }
        return CarStatus.SOLD;
    }
}
