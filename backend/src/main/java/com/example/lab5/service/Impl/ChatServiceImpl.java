package com.example.lab5.service.Impl;

import com.example.lab5.dto.ChatMessageRequest;
import com.example.lab5.dto.ChatMessageResponse;
import com.example.lab5.dto.ChatRoomRequest;
import com.example.lab5.dto.ChatRoomResponse;
import com.example.lab5.dto.ChatUserResponse;
import com.example.lab5.entity.User;
import com.example.lab5.mongodocument.ChatMessage;
import com.example.lab5.mongodocument.ChatRoom;
import com.example.lab5.mongorepository.ChatMessageRepository;
import com.example.lab5.mongorepository.ChatRoomRepository;
import com.example.lab5.repository.UserRepository;
import com.example.lab5.service.ChatService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Triển khai ChatService: quản lý phòng chat, lưu tin nhắn vào MongoDB,
 * broadcast qua STOMP WebSocket.
 */
@Service
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatServiceImpl(
            ChatRoomRepository chatRoomRepository,
            ChatMessageRepository chatMessageRepository,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public ChatRoomResponse getOrCreatePrivateRoom(Long currentUserId, Long otherUserId) {
        if (currentUserId.equals(otherUserId)) {
            throw new IllegalArgumentException("Không thể tạo phòng chat với chính mình");
        }

        // Tìm phòng PRIVATE đã tồn tại giữa 2 user
        return chatRoomRepository
                .findByTypeAndParticipantIdsContainingAndParticipantIdsContaining(
                        ChatRoom.ChatRoomType.PRIVATE, currentUserId, otherUserId)
                .map(room -> toResponse(room))
                .orElseGet(() -> {
                    // Kiểm tra user tồn tại
                    User otherUser = userRepository.findById(otherUserId)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Người dùng với ID " + otherUserId + " không tồn tại"));
                    if (!otherUser.isEnabled()) {
                        throw new IllegalArgumentException("Tài khoản này hiện không thể nhận tin nhắn");
                    }

                    ChatRoom room = ChatRoom.builder()
                            .type(ChatRoom.ChatRoomType.PRIVATE)
                            .participantIds(List.of(currentUserId, otherUserId))
                            .createdBy(currentUserId)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    return toResponse(chatRoomRepository.save(room));
                });
    }

    @Override
    public ChatRoomResponse createGroupRoom(Long currentUserId, ChatRoomRequest request) {
        // Đảm bảo danh sách participant bao gồm chính mình
        List<Long> participantIds = new ArrayList<>(request.getParticipantIds());
        if (!participantIds.contains(currentUserId)) {
            participantIds.add(currentUserId);
        }

        // Validate tất cả participant tồn tại
        List<User> users = userRepository.findAllById(participantIds);
        if (users.size() != participantIds.size()) {
            throw new IllegalArgumentException("Một hoặc nhiều người dùng không tồn tại");
        }

        ChatRoom room = ChatRoom.builder()
                .name(request.getName())
                .type(ChatRoom.ChatRoomType.GROUP)
                .participantIds(participantIds)
                .createdBy(currentUserId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return toResponse(chatRoomRepository.save(room));
    }

    @Override
    public List<ChatUserResponse> searchChatUsers(Long currentUserId, String keyword) {
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.length() < 2) {
            return List.of();
        }
        return userRepository
                .findTop20ByEnabledTrueAndIdNotAndUsernameContainingIgnoreCaseOrderByUsernameAsc(
                        currentUserId, normalized)
                .stream()
                .map(user -> new ChatUserResponse(
                        user.getId(), user.getUsername(), user.getRole().name()))
                .toList();
    }

    @Override
    public List<ChatRoomResponse> getRoomsForUser(Long userId) {
        return chatRoomRepository
                .findByParticipantIdsContainingOrderByLastMessageAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ChatMessageResponse> getMessages(String roomId, Long currentUserId, Pageable pageable) {
        // Kiểm tra quyền truy cập
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Phòng chat không tồn tại"));
        if (!room.getParticipantIds().contains(currentUserId)) {
            throw new SecurityException("Bạn không phải thành viên của phòng chat này");
        }

        return chatMessageRepository
                .findByRoomIdOrderByCreatedAtDesc(roomId, pageable)
                .map(ChatMessageResponse::from);
    }

    @Override
    public ChatMessageResponse sendMessage(Long senderId, ChatMessageRequest request) {
        // Kiểm tra phòng tồn tại và quyền truy cập
        ChatRoom room = chatRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Phòng chat không tồn tại"));
        if (!room.getParticipantIds().contains(senderId)) {
            throw new SecurityException("Bạn không phải thành viên của phòng chat này");
        }

        // Lấy username người gửi
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Người gửi không tồn tại"));

        // Lưu tin nhắn
        ChatMessage message = ChatMessage.builder()
                .roomId(request.getRoomId())
                .senderId(senderId)
                .senderUsername(sender.getUsername())
                .content(request.getContent())
                .type(ChatMessage.MessageType.CHAT)
                .createdAt(Instant.now())
                .build();
        ChatMessage saved = chatMessageRepository.save(message);

        // Cập nhật preview tin nhắn cuối trong phòng
        room.setLastMessageContent(request.getContent());
        room.setLastMessageAt(saved.getCreatedAt());
        room.setUpdatedAt(Instant.now());
        chatRoomRepository.save(room);

        ChatMessageResponse response = ChatMessageResponse.from(saved);

        // Broadcast tới topic của phòng qua STOMP
        messagingTemplate.convertAndSend("/topic/chat/" + request.getRoomId(), response);

        // Gửi notification tới từng participant (trừ người gửi) qua queue riêng
        room.getParticipantIds().stream()
                .filter(id -> !id.equals(senderId))
                .forEach(participantId ->
                        messagingTemplate.convertAndSend(
                                "/topic/user/" + participantId + "/chat-notifications",
                                response));

        return response;
    }

    /** Chuyển ChatRoom entity sang ChatRoomResponse DTO. */
    private ChatRoomResponse toResponse(ChatRoom room) {
        // Resolve username cho participant IDs
        Map<Long, String> userMap = userRepository.findAllById(room.getParticipantIds())
                .stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        List<String> usernames = room.getParticipantIds().stream()
                .map(id -> userMap.getOrDefault(id, "Unknown"))
                .collect(Collectors.toList());

        return ChatRoomResponse.from(room, usernames);
    }
}
