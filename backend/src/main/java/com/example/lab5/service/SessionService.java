package com.example.lab5.service;

import com.example.lab5.exception.BadRequestException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

// Redis: lưu token phiên đăng nhập với TTL
@Service
public class SessionService {

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final String KEY_PREFIX = "session:";

    private final StringRedisTemplate redisTemplate;

    public SessionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String createSession(Long userId) {
        if (userId == null) {
            throw new BadRequestException("Người dùng không được để trống");
        }
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(KEY_PREFIX + token, String.valueOf(userId), TTL);
        return token;
    }

    public Long getUserId(String token) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + token);
        if (value == null) {
            throw new BadRequestException("Phiên đăng nhập không tồn tại hoặc đã hết hạn");
        }
        return Long.valueOf(value);
    }

    public void invalidate(String token) {
        redisTemplate.delete(KEY_PREFIX + token);
    }
}
