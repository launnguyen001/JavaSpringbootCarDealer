package com.example.lab5.service;

import com.example.lab5.entity.Notification;
import com.example.lab5.entity.NotificationType;
import com.example.lab5.entity.Role;
import com.example.lab5.entity.User;
import com.example.lab5.exception.ForbiddenException;
import com.example.lab5.repository.NotificationRepository;
import com.example.lab5.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceTests {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User other;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(newUser("notification-owner"));
        other = userRepository.save(newUser("notification-other"));
    }

    @Test
    void listsOnlyOwnNotificationsNewestFirst() {
        notificationRepository.save(notification(owner, "Thông báo cũ", NotificationType.ORDER, true));
        notificationRepository.save(notification(owner, "Thông báo mới", NotificationType.PAYMENT, false));
        notificationRepository.save(notification(other, "Của người khác", NotificationType.SYSTEM, false));

        List<Notification> mine = notificationService.getMine(owner.getUsername());

        assertThat(mine).extracting(notification -> notification.getTitle())
                .containsExactly("Thông báo mới", "Thông báo cũ");
        assertThat(notificationService.countUnread(owner.getUsername())).isEqualTo(1);
    }

    @Test
    void marksSingleNotificationAsRead() {
        Notification notification = notificationRepository.save(
                notification(owner, "Đơn hàng đã xác nhận", NotificationType.ORDER, false));

        Notification updated = notificationService.markRead(notification.getId(), owner.getUsername());

        assertThat(updated.isRead()).isTrue();
        assertThat(updated.getReadAt()).isNotNull();
        assertThat(notificationService.countUnread(owner.getUsername())).isZero();
    }

    @Test
    void cannotReadAnotherUsersNotification() {
        Notification notification = notificationRepository.save(
                notification(owner, "Riêng tư", NotificationType.ORDER, false));

        assertThatThrownBy(() -> notificationService.markRead(notification.getId(), other.getUsername()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void marksAllNotificationsAsRead() {
        notificationRepository.save(notification(owner, "Một", NotificationType.ORDER, false));
        notificationRepository.save(notification(owner, "Hai", NotificationType.PAYMENT, false));

        int updated = notificationService.markAllRead(owner.getUsername());

        assertThat(updated).isEqualTo(2);
        assertThat(notificationService.countUnread(owner.getUsername())).isZero();
    }

    private User newUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("irrelevant");
        user.setRole(Role.USER);
        user.setEnabled(true);
        return user;
    }

    private Notification notification(User user, String title, NotificationType type, boolean read) {
        Notification notification = new Notification();
        notification.setRecipientUserId(user.getId());
        notification.setRecipientEmail(user.getEmail());
        notification.setTitle(title);
        notification.setMessage(title + " — nội dung kiểm thử");
        notification.setType(type);
        notification.setRead(read);
        return notification;
    }
}
