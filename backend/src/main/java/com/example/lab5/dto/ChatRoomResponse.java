package com.example.lab5.dto;

import com.example.lab5.mongodocument.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO phản hồi thông tin phòng chat.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomResponse {

    private String id;
    private String name;
    private ChatRoom.ChatRoomType type;
    private List<Long> participantIds;
    private List<String> participantUsernames;
    private Long createdBy;
    private Instant createdAt;
    private String lastMessageContent;
    private Instant lastMessageAt;

    public static ChatRoomResponse from(ChatRoom room, List<String> participantUsernames) {
        return ChatRoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .type(room.getType())
                .participantIds(room.getParticipantIds())
                .participantUsernames(participantUsernames)
                .createdBy(room.getCreatedBy())
                .createdAt(room.getCreatedAt())
                .lastMessageContent(room.getLastMessageContent())
                .lastMessageAt(room.getLastMessageAt())
                .build();
    }
}
