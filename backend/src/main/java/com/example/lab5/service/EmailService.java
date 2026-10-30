package com.example.lab5.service;

public interface EmailService {

    /** Gửi email HTML bất đồng bộ; lỗi SMTP chỉ được ghi log, không làm hỏng nghiệp vụ. */
    void send(String to, String subject, String htmlBody);

    default boolean isEnabled() {
        return true;
    }
}
