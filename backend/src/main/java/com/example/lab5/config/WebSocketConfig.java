package com.example.lab5.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Cấu hình WebSocket STOMP cho chat realtime.
 * - Endpoint kết nối: /ws
 * - Client gửi tin nhắn prefix: /app
 * - Server broadcast prefix: /topic (public), /queue (private)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    public WebSocketConfig(WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Broker đơn giản trong bộ nhớ (đủ cho quy mô nhỏ/vừa)
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix cho các message từ client
        config.setApplicationDestinationPrefixes("/app");
        // Prefix cho message riêng gửi tới user cụ thể
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint để client kết nối WebSocket
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // Fallback cho các browser cũ
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Interceptor xác thực JWT khi CONNECT
        registration.interceptors(webSocketAuthInterceptor);
    }
}
