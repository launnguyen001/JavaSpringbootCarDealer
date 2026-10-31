package com.example.lab5.controller;


import com.example.lab5.mongorepository.ChatRoomRepository;
import com.example.lab5.repository.UserRepository;
import com.example.lab5.service.ChatPresenceTracker;
import com.example.lab5.util.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

/** Snapshot presence chỉ gồm những người có chung phòng chat với user hiện tại. */
@RestController
@RequestMapping("/api/chat")
public class ChatPresenceController {
    private final UserRepository users;
    private final ChatRoomRepository rooms;
    private final ChatPresenceTracker presence;

    public ChatPresenceController(UserRepository users, ChatRoomRepository rooms,
                                  ChatPresenceTracker presence) {
        this.users = users;
        this.rooms = rooms;
        this.presence = presence;
    }

    @GetMapping("/online-users")
    public Set<Long> onlineUsers() {
        Long currentUserId = users.findByUsername(SecurityUtils.currentUsername())
                .map(user -> user.getId())
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));

        Set<Long> conversationUsers = rooms
                .findByParticipantIdsContainingOrderByLastMessageAtDesc(currentUserId)
                .stream()
                .map(room -> room.getParticipantIds())
                .flatMap(participantIds -> participantIds.stream())
                .filter(id -> !id.equals(currentUserId))
                .collect(Collectors.toSet());

        conversationUsers.retainAll(presence.onlineUserIds());
        return conversationUsers;
    }
}
