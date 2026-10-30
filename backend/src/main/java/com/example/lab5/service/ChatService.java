package com.example.lab5.service;

import com.example.lab5.dto.ChatMessageRequest;
import com.example.lab5.dto.ChatMessageResponse;
import com.example.lab5.dto.ChatRoomRequest;
import com.example.lab5.dto.ChatRoomResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service quản lý chat realtime: phòng chat, tin nhắn, WebSocket broadcast.
 */
public interface ChatService {

    /** Tạo hoặc lấy phòng chat PRIVATE giữa currentUser và user khác. */
    ChatRoomResponse getOrCreatePrivateRoom(Long currentUserId, Long otherUserId);

    /** Tạo phòng chat GROUP. */
    ChatRoomResponse createGroupRoom(Long currentUserId, ChatRoomRequest request);

    /** Lấy danh sách phòng chat của user. */
    List<ChatRoomResponse> getRoomsForUser(Long userId);

    /** Lấy lịch sử tin nhắn theo phòng (phân trang). */
    Page<ChatMessageResponse> getMessages(String roomId, Long currentUserId, Pageable pageable);

    /** Gửi tin nhắn và broadcast qua WebSocket. */
    ChatMessageResponse sendMessage(Long senderId, ChatMessageRequest request);
}
