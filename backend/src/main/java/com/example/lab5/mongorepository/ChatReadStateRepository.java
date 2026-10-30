package com.example.lab5.mongorepository;

import com.example.lab5.mongodocument.ChatReadState;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface ChatReadStateRepository extends MongoRepository<ChatReadState, String> {
    Optional<ChatReadState> findByRoomIdAndUserId(String roomId, Long userId);
    List<ChatReadState> findByRoomId(String roomId);
    List<ChatReadState> findByUserId(Long userId);
}
