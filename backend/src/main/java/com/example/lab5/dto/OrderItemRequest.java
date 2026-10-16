package com.example.lab5.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Một dòng trong đơn hàng: trỏ tới MỘT chiếc xe vật lý.
 *
 * <p>{@code quantity} giữ lại cho tương thích nhưng mỗi dòng chỉ ứng với một chiếc nên
 * bỏ trống hoặc bằng 1 (giá trị khác sẽ bị từ chối).
 */
public record OrderItemRequest(
        @NotNull(message = "Xe không được để trống")
        Long carItemId,

        Integer quantity
) {
}
