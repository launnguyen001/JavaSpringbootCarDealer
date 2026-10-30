package com.example.lab5.controller;

import com.example.lab5.dto.ChatMessageResponse;
import com.example.lab5.dto.ChatRoomRequest;
import com.example.lab5.dto.ChatRoomResponse;
import com.example.lab5.entity.User;
import com.example.lab5.repository.UserRepository;
import com.example.lab5.service.ChatService;
import com.example.lab5.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller cho chat: quản lý phòng chat và lịch sử tin nhắn.
 * WebSocket xử lý tin nhắn realtime, REST xử lý CRUD phòng + lịch sử.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    public ChatController(ChatService chatService, UserRepository userRepository) {
        this.chatService = chatService;
        this.userRepository = userRepository;
    }

    /** Lấy danh sách phòng chat của user đang đăng nhập. */
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomResponse>> getMyRooms() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(chatService.getRoomsForUser(userId));
    }

    /** Tạo hoặc lấy phòng chat PRIVATE với user khác. */
    @PostMapping("/rooms/private/{otherUserId}")
    public ResponseEntity<ChatRoomResponse> getOrCreatePrivateRoom(
            @PathVariable Long otherUserId) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(chatService.getOrCreatePrivateRoom(userId, otherUserId));
    }

    /** Tạo phòng chat GROUP. */
    @PostMapping("/rooms/group")
    public ResponseEntity<ChatRoomResponse> createGroupRoom(
            @Valid @RequestBody ChatRoomRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(chatService.createGroupRoom(userId, request));
    }

    /** Lấy lịch sử tin nhắn của phòng chat (phân trang). */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Page<ChatMessageResponse>> getMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(chatService.getMessages(roomId, userId, pageable));
    }

    /** Trích xuất userId từ SecurityContext. */
    private Long getCurrentUserId() {
        String username = SecurityUtils.currentUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));
        return user.getId();
    }
}
