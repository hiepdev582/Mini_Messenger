package com.hiepnn.mini_messeger.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    @Id
    private Long id; // Unique Snowflake ID

    private Long senderId;
    private Long recipientId;
    private String content;
    private String mediaUrl;
    private Long timestamp;
    private MessageStatus status;

    public enum MessageStatus {
        SENT, DELIVERED, READ
    }
}
