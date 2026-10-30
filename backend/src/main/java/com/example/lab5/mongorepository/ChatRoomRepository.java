package com.example.lab5.mongorepository;

import com.example.lab5.mongodocument.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {

    /** Tìm tất cả phòng chat mà user tham gia, sắp xếp theo tin nhắn mới nhất. */
    List<ChatRoom> findByParticipantIdsContainingOrderByLastMessageAtDesc(Long userId);

    /**
     * Tìm đúng phòng PRIVATE giữa hai user.
     *
     * Dùng $all thay vì derived query có hai lần Containing trên cùng field.
     * MongoDB Document không cho phép Criteria chứa hai biểu thức participantIds
     * riêng biệt (mỗi biểu thức $in), gây InvalidMongoDbApiUsageException.
     * $size: 2 bảo đảm đây là phòng 1-1, không phải phòng nhiều thành viên.
     */
    @Query(value = "{ 'type': ?0, 'participantIds': { $all: [?1, ?2], $size: 2 } }")
    Optional<ChatRoom> findPrivateRoomBetween(
            ChatRoom.ChatRoomType type, Long userId1, Long userId2);
}
