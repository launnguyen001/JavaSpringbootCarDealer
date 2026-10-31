package com.example.lab5.controller;

import com.example.lab5.dto.ChatMessageRequest;
import com.example.lab5.entity.User;
import com.example.lab5.repository.UserRepository;
import com.example.lab5.service.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

/**
 * WebSocket STOMP controller cho chat realtime.
 * Client gửi tin nhắn tới /app/chat.send → server xử lý → broadcast tới /topic/chat/{roomId}.
 */
@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    public ChatWebSocketController(ChatService chatService, UserRepository userRepository) {
        this.chatService = chatService;
        this.userRepository = userRepository;
    }

    /**
     * Nhận tin nhắn từ client qua STOMP destination /app/chat.send.
     * Tự động broadcast kết quả tới /topic/chat/{roomId} (xử lý trong ChatServiceImpl).
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest request,
                            SimpMessageHeaderAccessor headerAccessor) {
        Long userId = extractUserId(headerAccessor);
        chatService.sendMessage(userId, request);
    }

    /** Trích xuất userId từ WebSocket session (đã xác thực qua JWT interceptor). */
    private Long extractUserId(SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor.getUser() == null) {
            throw new SecurityException("WebSocket chưa xác thực");
        }
        java.security.Principal principal = headerAccessor.getUser();
        String username;
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken authToken) {
            Object userDetails = authToken.getPrincipal();
            if (userDetails instanceof UserDetails ud) {
                username = ud.getUsername();
            } else {
                username = authToken.getName();
            }
        } else {
            username = principal.getName();
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại: " + username));
        return user.getId();
    }
}
