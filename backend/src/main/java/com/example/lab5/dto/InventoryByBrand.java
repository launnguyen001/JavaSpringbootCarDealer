package com.example.lab5.dto;

/**
 * Tồn kho theo hãng: {@code totalCars} = số dòng xe (xe sản phẩm), {@code totalQuantity} = số chiếc vật lý,
 * {@code availableItems} = số chiếc đang sẵn sàng.
 */
public record InventoryByBrand(
        Long brandId,
        String brandName,
        Long totalCars,
        Long totalQuantity,
        Long availableItems
) {
}
