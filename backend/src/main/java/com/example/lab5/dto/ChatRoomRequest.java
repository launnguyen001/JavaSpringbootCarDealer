package com.example.lab5.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO yêu cầu tạo phòng chat.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomRequest {

    /** Tên phòng (bắt buộc cho GROUP, tùy chọn cho PRIVATE). */
    private String name;

    /** Danh sách userId (MySQL User.id) tham gia phòng. Không cần bao gồm chính mình. */
    @NotEmpty(message = "Phải có ít nhất một người tham gia")
    private List<Long> participantIds;
}
