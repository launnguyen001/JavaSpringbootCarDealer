package com.example.lab5.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho tin nhắn gửi qua WebSocket (client → server).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageRequest {

    @NotBlank(message = "roomId không được để trống")
    private String roomId;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String content;
}
