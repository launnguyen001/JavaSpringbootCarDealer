package com.example.lab5.dto;

import jakarta.validation.constraints.NotNull;

/** Admin bật/tắt trạng thái hoạt động của một tài khoản. */
public record UserStatusRequest(
        // Dùng Boolean (không phải boolean) để bắt buộc client gửi giá trị:
        // nếu để primitive thì body rỗng sẽ mặc định false và vô tình khoá tài khoản.
        @NotNull(message = "Trạng thái không được để trống")
        Boolean enabled
) {
}
