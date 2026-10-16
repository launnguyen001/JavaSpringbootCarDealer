package com.example.lab5.service;

import com.example.lab5.dto.CartItem;
import com.example.lab5.exception.BadRequestException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Redis: giỏ cấu hình xe theo phiên (dữ liệu tạm, có TTL) - lưu bằng Redis Hash
@Service
public class ConfiguratorCartService {

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final String KEY_PREFIX = "cart:";

    private final StringRedisTemplate redisTemplate;

    public ConfiguratorCartService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public List<CartItem> addItem(String sessionId, Long carId, Integer quantity) {
        validate(sessionId, carId, quantity);
        String key = KEY_PREFIX + sessionId;
        redisTemplate.opsForHash().put(key, carId.toString(), quantity.toString());
        redisTemplate.expire(key, TTL);
        return getCart(sessionId);
    }

    public List<CartItem> getCart(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BadRequestException("Mã phiên không được để trống");
        }
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(KEY_PREFIX + sessionId);
        List<CartItem> items = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            items.add(new CartItem(
                    Long.valueOf(entry.getKey().toString()),
                    Integer.valueOf(entry.getValue().toString())
            ));
        }
        return items;
    }

    public List<CartItem> removeItem(String sessionId, Long carId) {
        if (carId == null) {
            throw new BadRequestException("Xe không được để trống");
        }
        redisTemplate.opsForHash().delete(KEY_PREFIX + sessionId, carId.toString());
        return getCart(sessionId);
    }

    public void clear(String sessionId) {
        redisTemplate.delete(KEY_PREFIX + sessionId);
    }

    private void validate(String sessionId, Long carId, Integer quantity) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BadRequestException("Mã phiên không được để trống");
        }
        if (carId == null) {
            throw new BadRequestException("Xe không được để trống");
        }
        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Số lượng phải lớn hơn 0");
        }
    }
}
