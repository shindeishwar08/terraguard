package com.terraguard.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraguard.backend.cache.CacheService;
import com.terraguard.backend.domain.enums.SignalType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final CacheService cacheService;
    private final KeywordMatcherService keywordMatcherService;
    private final ObjectMapper objectMapper;

    @MessageMapping("/incidents/{incidentId}/chat")
    public void handleMessage(
            @DestinationVariable String incidentId,
            @Payload ChatMessage message) {

        try {
            // 1. Set server-side fields
            message.setIncidentId(incidentId);
            message.setTimestamp(OffsetDateTime.now());

            // 2. Keyword check — set type
            boolean isHighlight = keywordMatcherService.isHighlightWorthy(message.getContent());
            message.setType(isHighlight ? ChatMessage.MessageType.HIGHLIGHT : ChatMessage.MessageType.NORMAL);

            // 3. Serialize and save to Redis
            String json = objectMapper.writeValueAsString(message);
            cacheService.appendChatMessage(incidentId, json);

            // 3b. If tagged — increment signal tally
            if (message.getTag() != null && !message.getTag().isBlank()) {
                try {
                    SignalType signalType = SignalType.valueOf(message.getTag());
                    cacheService.incrementSignal(incidentId, signalType);
                } catch (IllegalArgumentException e) {
                    log.warn("[CHAT] Unknown signal tag: {}", message.getTag());
                }
            }

            // 4. Broadcast to all subscribers of this incident room
            messagingTemplate.convertAndSend("/topic/incidents/" + incidentId, message);

            log.debug("[CHAT] {} → incident {}: {}", message.getDisplayName(), incidentId, message.getContent());

        } catch (Exception e) {
            log.error("[CHAT] Failed to handle message for incident {}", incidentId, e);
        }
    }

    @SubscribeMapping("/incidents/{incidentId}/history")
    public List<ChatMessage> sendHistory(@DestinationVariable String incidentId) {
        try {
            List<String> rawMessages = cacheService.getRecentMessages(incidentId);

            return rawMessages.stream()
                    .map(raw -> {
                        try {
                            return objectMapper.readValue(raw, ChatMessage.class);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(m -> m != null)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[CHAT] Failed to send history for incident {}", incidentId, e);
            return Collections.emptyList();
        }
    }
}