package com.example.lab5.mongodocument;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

/** Read cursor theo từng user trong từng phòng chat. */
@Document(collection = "chat_read_states")
@CompoundIndex(name = "room_user_read_idx", def = "{'roomId': 1, 'userId': 1}", unique = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatReadState {
    @Id
    private String id;
    private String roomId;
    private Long userId;
    private String lastReadMessageId;
    private Instant lastReadAt;
}
