package com.example.lab5.service.Impl;

import com.example.lab5.entity.Notification;
import com.example.lab5.entity.User;
import com.example.lab5.exception.ForbiddenException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.NotificationRepository;
import com.example.lab5.repository.UserRepository;
import com.example.lab5.service.EmailService;
import com.example.lab5.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final boolean emailEnabled;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            EmailService emailService,
            @Value("${app.notification.email-enabled:true}") boolean emailEnabled
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.emailEnabled = emailEnabled;
    }

    /**
     * Chạy trong transaction mới vì thường được gọi từ listener AFTER_COMMIT
     * (transaction nghiệp vụ gốc đã đóng).
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(Notification notification) {
        notificationRepository.save(notification);
        if (emailEnabled) {
            emailService.send(notification.getRecipientEmail(), notification.getTitle(), notification.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getMine(String username) {
        User user = findUser(username);
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(String username) {
        User user = findUser(username);
        return notificationRepository.countByRecipientUserIdAndReadFalse(user.getId());
    }

    @Override
    @Transactional
    public Notification markRead(Long id, String username) {
        User user = findUser(username);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo có id = " + id));
        if (!user.getId().equals(notification.getRecipientUserId())) {
            throw new ForbiddenException("Bạn không có quyền đọc thông báo này");
        }
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        return notification;
    }

    @Override
    @Transactional
    public int markAllRead(String username) {
        User user = findUser(username);
        return notificationRepository.markAllRead(user.getId(), LocalDateTime.now());
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + username));
    }
}
