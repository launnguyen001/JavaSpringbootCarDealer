package com.example.lab5.mongorepository;

import com.example.lab5.mongodocument.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    /** Lấy lịch sử tin nhắn theo phòng, phân trang, mới nhất trước. */
    Page<ChatMessage> findByRoomIdOrderByCreatedAtDesc(String roomId, Pageable pageable);

    /** Đếm tin nhắn trong phòng (cho thống kê). */
    long countByRoomId(String roomId);
}
