package com.example.lab5.dto;

import com.example.lab5.entity.CarCondition;
import com.example.lab5.entity.CarStatus;

import java.util.List;

/**
 * Dòng xe trả về cho danh sách (kèm số liệu tổng hợp theo từng chiếc).
 *
 * <p>Giữ đủ dữ liệu để UI danh sách/theo lô không phải đổi nhiều:
 * giá từ {@code priceFrom}..{@code priceTo} (theo các chiếc thực tế), số chiếc theo trạng thái,
 * và danh sách tình trạng (NEW/USED) đang có.
 *
 * <p>{@code items} chỉ được điền ở API chi tiết ({@code GET /api/cars/{id}}); ở danh sách để rỗng.
 */
public record CarResponse(
        Long id,
        String name,
        String description,
        Double basePrice,
        Double priceFrom,
        Double priceTo,
        long totalItems,
        long availableItems,
        long reservedItems,
        long soldItems,
        long maintenanceItems,
        List<CarCondition> conditions,
        CarStatus status,
        Long modelId,
        String modelName,
        String brandName,
        List<CarItemResponse> items
) {
}
