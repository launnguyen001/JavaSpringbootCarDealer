package com.example.lab5.controller;

import com.example.lab5.service.SessionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Redis: quản lý phiên đăng nhập
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public Map<String, String> createSession(@RequestParam Long userId) {
        return Map.of("token", sessionService.createSession(userId));
    }

    @GetMapping("/{token}")
    public Map<String, Long> getUserId(@PathVariable String token) {
        return Map.of("userId", sessionService.getUserId(token));
    }

    @DeleteMapping("/{token}")
    public void invalidate(@PathVariable String token) {
        sessionService.invalidate(token);
    }
}
