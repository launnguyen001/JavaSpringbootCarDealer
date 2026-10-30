package com.example.lab5.mongodocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Phòng chat lưu trong MongoDB.
 * Mỗi phòng chứa danh sách participantIds (User.id từ MySQL).
 * Hỗ trợ chat 1-1 (khách ↔ nhân viên/admin) hoặc nhóm.
 */
@Document(collection = "chat_rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    private String id;

    /** Tên hiển thị phòng chat (nullable cho chat 1-1). */
    private String name;

    /** Loại phòng: PRIVATE (1-1) hoặc GROUP. */
    @Indexed
    @Builder.Default
    private ChatRoomType type = ChatRoomType.PRIVATE;

    /** Danh sách userId (MySQL User.id) tham gia phòng. */
    @Indexed
    @Builder.Default
    private List<Long> participantIds = new ArrayList<>();

    /** User tạo phòng. */
    private Long createdBy;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();

    /** Nội dung tin nhắn cuối cùng (để hiển thị preview). */
    private String lastMessageContent;

    /** Thời điểm tin nhắn cuối cùng. */
    private Instant lastMessageAt;

    public enum ChatRoomType {
        PRIVATE, GROUP
    }
}
