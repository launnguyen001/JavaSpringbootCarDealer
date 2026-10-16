package com.example.lab5.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

// CORS cho frontend dev (Next.js chạy ở :3000/:3001).
// Kiến trúc chính vẫn là Next rewrite '/api/:path*' -> http://localhost:8080 (same-origin, không cần CORS);
// cấu hình này là lưới an toàn khi gọi trực tiếp backend từ trình duyệt.
// Danh sách origin cấu hình qua 'app.cors.allowed-origins' trong application.properties.
@Configuration
public class WebCorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000,http://localhost:3001,http://127.0.0.1:3001}")
            List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        // Dùng allowedOriginPatterns để không vỡ khi port thay đổi (vd: 3000 -> 3002)
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
