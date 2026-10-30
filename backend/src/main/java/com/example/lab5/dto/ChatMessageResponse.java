package com.example.lab5.dto;

import com.example.lab5.mongodocument.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO phản hồi tin nhắn chat (server → client).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {

    private String id;
    private String roomId;
    private Long senderId;
    private String senderUsername;
    private String content;
    private ChatMessage.MessageType type;
    private Instant createdAt;

    public static ChatMessageResponse from(ChatMessage msg) {
        return ChatMessageResponse.builder()
                .id(msg.getId())
                .roomId(msg.getRoomId())
                .senderId(msg.getSenderId())
                .senderUsername(msg.getSenderUsername())
                .content(msg.getContent())
                .type(msg.getType())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
