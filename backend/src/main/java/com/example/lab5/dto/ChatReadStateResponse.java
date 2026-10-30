package com.example.lab5.dto;

import com.example.lab5.mongodocument.ChatReadState;
import java.time.Instant;

public record ChatReadStateResponse(
        Long userId,
        String lastReadMessageId,
        Instant lastReadAt
) {
    public static ChatReadStateResponse from(ChatReadState state) {
        return new ChatReadStateResponse(
                state.getUserId(), state.getLastReadMessageId(), state.getLastReadAt());
    }
}
