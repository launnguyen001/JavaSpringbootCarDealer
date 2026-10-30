package com.example.lab5.controller;

import com.example.lab5.dto.NotificationResponse;
import com.example.lab5.mapper.NotificationMapper;
import com.example.lab5.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    public NotificationController(NotificationService notificationService, NotificationMapper notificationMapper) {
        this.notificationService = notificationService;
        this.notificationMapper = notificationMapper;
    }

    /** Thông báo của tài khoản đang đăng nhập, mới nhất trước. */
    @GetMapping
    public List<NotificationResponse> getMine(@AuthenticationPrincipal UserDetails userDetails) {
        return notificationService.getMine(userDetails.getUsername()).stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> countUnread(@AuthenticationPrincipal UserDetails userDetails) {
        return Map.of("unread", notificationService.countUnread(userDetails.getUsername()));
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return notificationMapper.toResponse(notificationService.markRead(id, userDetails.getUsername()));
    }

    @PatchMapping("/read-all")
    public Map<String, Integer> markAllRead(@AuthenticationPrincipal UserDetails userDetails) {
        return Map.of("updated", notificationService.markAllRead(userDetails.getUsername()));
    }
}
