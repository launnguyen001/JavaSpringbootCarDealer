package com.example.lab5.controller;

import com.example.lab5.dto.CartItem;
import com.example.lab5.service.ConfiguratorCartService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Redis: giỏ cấu hình xe (khoá là id chiếc xe vật lý)
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final ConfiguratorCartService cartService;

    public CartController(ConfiguratorCartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    public List<CartItem> addItem(
            @RequestParam String sessionId,
            @RequestParam Long carItemId,
            @RequestParam Integer quantity
    ) {
        return cartService.addItem(sessionId, carItemId, quantity);
    }

    @GetMapping
    public List<CartItem> getCart(@RequestParam String sessionId) {
        return cartService.getCart(sessionId);
    }

    @DeleteMapping("/items")
    public List<CartItem> removeItem(
            @RequestParam String sessionId,
            @RequestParam Long carItemId
    ) {
        return cartService.removeItem(sessionId, carItemId);
    }

    @DeleteMapping
    public void clear(@RequestParam String sessionId) {
        cartService.clear(sessionId);
    }
}
