package com.example.lab5.mongorepository;

import com.example.lab5.mongodocument.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {

    /** Tìm tất cả phòng chat mà user tham gia, sắp xếp theo tin nhắn mới nhất. */
    List<ChatRoom> findByParticipantIdsContainingOrderByLastMessageAtDesc(Long userId);

    /** Tìm phòng chat PRIVATE giữa 2 user. */
    Optional<ChatRoom> findByTypeAndParticipantIdsContainingAndParticipantIdsContaining(
            ChatRoom.ChatRoomType type, Long userId1, Long userId2);
}
