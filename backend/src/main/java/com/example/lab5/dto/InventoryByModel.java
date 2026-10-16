package com.example.lab5.dto;

/**
 * Tồn kho theo dòng xe (model): {@code totalCars} = số xe sản phẩm, {@code totalQuantity} = số chiếc vật lý,
 * {@code availableItems} = số chiếc đang sẵn sàng.
 */
public record InventoryByModel(
        Long modelId,
        String modelName,
        String bodyType,
        Long totalCars,
        Long totalQuantity,
        Long availableItems
) {
}
