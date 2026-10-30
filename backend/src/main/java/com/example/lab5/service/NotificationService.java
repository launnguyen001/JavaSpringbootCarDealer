package com.example.lab5.service;

import com.example.lab5.entity.Notification;

import java.util.List;

public interface NotificationService {

    /** Lưu thông báo in-app và gửi email (nếu khách có email). */
    void create(Notification notification);

    List<Notification> getMine(String username);

    long countUnread(String username);

    Notification markRead(Long id, String username);

    int markAllRead(String username);
}
