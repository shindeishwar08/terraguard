package com.terraguard.backend.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    public enum MessageType {
        NORMAL, HIGHLIGHT
    }

    private String incidentId;
    private String displayName;
    private String content;
    private String tag;
    private MessageType type;
    private OffsetDateTime timestamp;
}