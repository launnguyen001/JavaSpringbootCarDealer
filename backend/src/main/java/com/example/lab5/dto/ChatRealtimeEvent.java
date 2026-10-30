package com.example.lab5.dto;

import java.time.Instant;

/** Sự kiện realtime không phải tin nhắn chat và không được lưu trong chat_messages. */
public record ChatRealtimeEvent(
        String type,
        String roomId,
        Long userId,
        String username,
        Boolean typing,
        String lastReadMessageId,
        Instant lastReadAt
) {}
