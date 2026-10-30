package com.example.lab5.event;

import com.example.lab5.entity.NotificationType;

/**
 * Sự kiện nghiệp vụ cần thông báo cho khách hàng.
 *
 * <p>Sự kiện chỉ chứa dữ liệu phẳng (không giữ entity/proxy) để listener có thể xử lý
 * sau khi transaction gốc đã commit mà không gặp LazyInitializationException.
 */
public record BusinessEvent(
        NotificationType type,
        String title,
        String message,
        String entityType,
        Long entityId,
        Long recipientUserId,
        String recipientEmail,
        String recipientName
) {
}
