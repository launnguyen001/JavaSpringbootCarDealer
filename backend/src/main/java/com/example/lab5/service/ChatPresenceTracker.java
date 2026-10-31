package com.example.lab5.service;

import com.example.lab5.repository.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Theo dõi trạng thái online theo STOMP session.
 * Một user chỉ offline khi session cuối cùng (tab/thiết bị cuối) ngắt kết nối.
 * Bản in-memory phù hợp single-instance; multi-instance cần Redis presence.
 */
@Component
public class ChatPresenceTracker {
    private final UserRepository users;
    private final SimpMessagingTemplate broker;
    private final ConcurrentHashMap<Long, Set<String>> sessionsByUser = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> userBySession = new ConcurrentHashMap<>();

    public ChatPresenceTracker(UserRepository users, SimpMessagingTemplate broker) {
        this.users = users;
        this.broker = broker;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        String sessionId = accessor.getSessionId();
        if (principal == null || sessionId == null) return;

        String username = principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof UserDetails details
                ? details.getUsername() : principal.getName();

        users.findByUsername(username).map(user -> user.getId()).ifPresent(userId -> {
            Set<String> sessions = sessionsByUser.computeIfAbsent(
                    userId, ignored -> ConcurrentHashMap.newKeySet());
            boolean wasOffline = sessions.isEmpty();
            sessions.add(sessionId);
            userBySession.put(sessionId, userId);
            if (wasOffline) publish(userId, true);
        });
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        Long userId = userBySession.remove(event.getSessionId());
        if (userId == null) return;

        Set<String> sessions = sessionsByUser.get(userId);
        if (sessions != null) {
            sessions.remove(event.getSessionId());
            if (sessions.isEmpty() && sessionsByUser.remove(userId, sessions)) {
                publish(userId, false);
            }
        }
    }

    public Set<Long> onlineUserIds() {
        return Set.copyOf(sessionsByUser.keySet());
    }

    private void publish(Long userId, boolean online) {
        broker.convertAndSend("/topic/chat-presence", new PresenceEvent(userId, online));
    }

    public record PresenceEvent(Long userId, boolean online) {}
}
