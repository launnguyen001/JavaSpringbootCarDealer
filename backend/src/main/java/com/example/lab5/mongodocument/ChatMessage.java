package com.example.lab5.mongodocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Tin nhắn chat lưu trong MongoDB.
 * Index compound (roomId, createdAt) để truy vấn nhanh lịch sử tin nhắn theo phòng.
 */
@Document(collection = "chat_messages")
@CompoundIndex(name = "room_time_idx", def = "{'roomId': 1, 'createdAt': -1}")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    private String id;

    /** ID phòng chat (ChatRoom.id). */
    @Indexed
    private String roomId;

    /** ID người gửi (MySQL User.id). */
    private Long senderId;

    /** Username người gửi (denormalize để hiển thị nhanh). */
    private String senderUsername;

    /** Nội dung tin nhắn. */
    private String content;

    /** Loại tin nhắn. */
    @Builder.Default
    private MessageType type = MessageType.CHAT;

    @Builder.Default
    private Instant createdAt = Instant.now();

    public enum MessageType {
        CHAT,    // Tin nhắn bình thường
        JOIN,    // Thông báo người dùng vào phòng
        LEAVE    // Thông báo người dùng rời phòng
    }
}
