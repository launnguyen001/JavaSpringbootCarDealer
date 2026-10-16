package com.example.lab5.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Form tạo/sửa DÒNG XE (cars). Sau khi tách bảng, dòng xe chỉ còn thông tin sản phẩm:
 * tên, mô tả, giá niêm yết và dòng xe. VIN / màu / năm / tình trạng / trạng thái / giá bán
 * được quản lý ở từng chiếc (xem {@link CarItemRequest}).
 */
public record CarRequest(
        @NotBlank(message = "Tên xe không được để trống")
        @Size(max = 100, message = "Tên xe không được vượt quá 100 ký tự")
        String name,

        @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
        String description,

        @NotNull(message = "Giá niêm yết không được để trống")
        @PositiveOrZero(message = "Giá niêm yết không hợp lệ")
        Double basePrice,

        @NotNull(message = "Dòng xe không được để trống")
        Long modelId
) {
}
