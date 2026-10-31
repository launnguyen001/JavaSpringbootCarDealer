package com.example.lab5.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Interceptor xác thực JWT cho kết nối WebSocket STOMP.
 * Client gửi JWT token qua STOMP header "Authorization" hoặc qua query param "token"
 * khi thực hiện CONNECT.
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    public WebSocketAuthInterceptor(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);
            if (token == null || token.isBlank()) {
                throw new AccessDeniedException("WebSocket yêu cầu JWT authentication");
            }
            try {
                String username = jwtUtils.extractUsername(token);
                if (username == null) throw new AccessDeniedException("JWT không hợp lệ");
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (!jwtUtils.validateToken(token, userDetails)) {
                    throw new AccessDeniedException("JWT hết hạn hoặc không hợp lệ");
                }
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                accessor.setUser(authToken);
            } catch (AccessDeniedException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new AccessDeniedException("Không thể xác thực WebSocket", ex);
            }
        }

        return message;
    }

    /** Trích xuất JWT token từ STOMP header Authorization hoặc query param. */
    private String extractToken(StompHeaderAccessor accessor) {
        // Thử lấy từ STOMP native header "Authorization"
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }

        // Thử lấy từ STOMP native header "token" (cho SockJS/frontend dễ gửi)
        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0);
        }

        return null;
    }
}
