package com.example.lab5.controller;

import com.example.lab5.dto.ChatRealtimeEvent;
import com.example.lab5.entity.User;
import com.example.lab5.mongodocument.ChatReadState;
import com.example.lab5.mongodocument.ChatRoom;
import com.example.lab5.mongorepository.ChatMessageRepository;
import com.example.lab5.mongorepository.ChatReadStateRepository;
import com.example.lab5.mongorepository.ChatRoomRepository;
import com.example.lab5.repository.UserRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import java.security.Principal;

@Controller
public class ChatStateWebSocketController {
    private final UserRepository users;
    private final ChatRoomRepository rooms;
    private final ChatMessageRepository messages;
    private final ChatReadStateRepository readStates;
    private final SimpMessagingTemplate broker;

    public ChatStateWebSocketController(UserRepository users, ChatRoomRepository rooms,
            ChatMessageRepository messages, ChatReadStateRepository readStates, SimpMessagingTemplate broker) {
        this.users = users; this.rooms = rooms; this.messages = messages;
        this.readStates = readStates; this.broker = broker;
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingRequest request, SimpMessageHeaderAccessor accessor) {
        Long userId = currentUserId(accessor);
        ChatRoom room = requireMember(request.roomId(), userId);
        User user = users.findById(userId).orElseThrow();
        broker.convertAndSend("/topic/chat/" + room.getId() + "/events",
                new ChatRealtimeEvent("TYPING", room.getId(), userId, user.getUsername(),
                        Boolean.TRUE.equals(request.typing()), null, null));
    }

    @MessageMapping("/chat.read")
    public void read(@Payload ReadRequest request, SimpMessageHeaderAccessor accessor) {
        Long userId = currentUserId(accessor);
        ChatRoom room = requireMember(request.roomId(), userId);
        if (request.lastReadMessageId() == null || request.lastReadMessageId().isBlank()) return;
        var message = messages.findById(request.lastReadMessageId())
                .orElseThrow(() -> new IllegalArgumentException("Tin nhắn không tồn tại"));
        if (!room.getId().equals(message.getRoomId())) throw new IllegalArgumentException("Tin nhắn không thuộc phòng chat");
        ChatReadState state = readStates.findByRoomIdAndUserId(room.getId(), userId)
                .orElseGet(() -> ChatReadState.builder().roomId(room.getId()).userId(userId).build());
        if (state.getLastReadAt() != null && !message.getCreatedAt().isAfter(state.getLastReadAt())) return;
        state.setLastReadMessageId(message.getId());
        state.setLastReadAt(message.getCreatedAt());
        readStates.save(state);
        User user = users.findById(userId).orElseThrow();
        broker.convertAndSend("/topic/chat/" + room.getId() + "/events",
                new ChatRealtimeEvent("READ", room.getId(), userId, user.getUsername(),
                        null, message.getId(), message.getCreatedAt()));
    }

    private ChatRoom requireMember(String roomId, Long userId) {
        ChatRoom room = rooms.findById(roomId).orElseThrow(() -> new IllegalArgumentException("Phòng chat không tồn tại"));
        if (!room.getParticipantIds().contains(userId)) throw new SecurityException("Không có quyền trong phòng chat");
        return room;
    }

    private Long currentUserId(SimpMessageHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal == null) throw new SecurityException("WebSocket chưa xác thực");
        String username = principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof UserDetails details ? details.getUsername() : principal.getName();
        return users.findByUsername(username).orElseThrow(() -> new SecurityException("Người dùng không tồn tại")).getId();
    }

    public record TypingRequest(String roomId, Boolean typing) {}
    public record ReadRequest(String roomId, String lastReadMessageId) {}
}
