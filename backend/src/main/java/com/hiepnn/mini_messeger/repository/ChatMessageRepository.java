package com.hiepnn.mini_messeger.repository;

import com.hiepnn.mini_messeger.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, Long> {
    List<ChatMessage> findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderByIdAsc(
            Long senderId1, Long recipientId1, Long senderId2, Long recipientId2
    );
}
