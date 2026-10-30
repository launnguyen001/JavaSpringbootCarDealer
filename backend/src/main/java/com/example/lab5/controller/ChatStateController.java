package com.example.lab5.controller;

import com.example.lab5.dto.ChatReadStateResponse;
import com.example.lab5.entity.User;
import com.example.lab5.mongodocument.ChatReadState;
import com.example.lab5.mongodocument.ChatRoom;
import com.example.lab5.mongorepository.ChatMessageRepository;
import com.example.lab5.mongorepository.ChatReadStateRepository;
import com.example.lab5.mongorepository.ChatRoomRepository;
import com.example.lab5.repository.UserRepository;
import com.example.lab5.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

/** REST endpoints cho unread counters và read receipts. */
@RestController
@RequestMapping("/api/chat")
public class ChatStateController {
    private final UserRepository users;
    private final ChatRoomRepository rooms;
    private final ChatMessageRepository messages;
    private final ChatReadStateRepository readStates;

    public ChatStateController(UserRepository users, ChatRoomRepository rooms,
                               ChatMessageRepository messages, ChatReadStateRepository readStates) {
        this.users = users;
        this.rooms = rooms;
        this.messages = messages;
        this.readStates = readStates;
    }

    @GetMapping("/unread-counts")
    public ResponseEntity<Map<String, Long>> unreadCounts() {
        Long userId = currentUserId();
        Map<String, Long> result = new HashMap<>();
        for (ChatRoom room : rooms.findByParticipantIdsContainingOrderByLastMessageAtDesc(userId)) {
            Instant cursor = readStates.findByRoomIdAndUserId(room.getId(), userId)
                    .map(ChatReadState::getLastReadAt).orElse(Instant.EPOCH);
            long count = messages.countByRoomIdAndSenderIdNotAndCreatedAtAfter(room.getId(), userId, cursor);
            if (count > 0) result.put(room.getId(), count);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/rooms/{roomId}/read-states")
    public ResponseEntity<List<ChatReadStateResponse>> readStates(@PathVariable String roomId) {
        Long userId = currentUserId();
        ChatRoom room = rooms.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Phòng chat không tồn tại"));
        if (!room.getParticipantIds().contains(userId)) throw new SecurityException("Không có quyền truy cập phòng chat");
        return ResponseEntity.ok(readStates.findByRoomId(roomId).stream()
                .map(ChatReadStateResponse::from).toList());
    }

    private Long currentUserId() {
        String username = SecurityUtils.currentUsername();
        User user = users.findByUsername(username)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));
        return user.getId();
    }
}
