package com.example.lab5.event;

import com.example.lab5.config.AsyncConfig;
import com.example.lab5.entity.Notification;
import com.example.lab5.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Nhận sự kiện nghiệp vụ SAU KHI transaction commit rồi lưu thông báo + gửi email.
 *
 * <p>Chỉ thông báo khi dữ liệu đã thực sự được lưu (tránh gửi mail cho giao dịch bị rollback).
 * Chạy bất đồng bộ để không làm chậm response của API.
 */
@Component
public class BusinessEventListener {

    private static final Logger log = LoggerFactory.getLogger(BusinessEventListener.class);

    private final NotificationService notificationService;

    public BusinessEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(BusinessEvent event) {
        try {
            Notification notification = new Notification();
            notification.setRecipientUserId(event.recipientUserId());
            notification.setRecipientEmail(event.recipientEmail());
            notification.setType(event.type());
            notification.setTitle(event.title());
            notification.setMessage(event.message());
            notification.setEntityType(event.entityType());
            notification.setEntityId(event.entityId());
            notificationService.create(notification);
        } catch (Exception exception) {
            log.warn("Không thể tạo thông báo '{}': {}", event.title(), exception.getMessage());
        }
    }
}
