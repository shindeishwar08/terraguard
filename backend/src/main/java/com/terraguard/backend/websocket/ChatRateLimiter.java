package com.terraguard.backend.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class ChatRateLimiter implements ChannelInterceptor {

    private static final int MAX_MESSAGES_PER_MINUTE = 10;
    private static final int MAX_MESSAGE_SIZE = 500;
    private static final int MAX_CONCURRENT_CONNECTIONS = 200;

    // sessionId → message count this minute
    private final Map<String, AtomicInteger> messageCounters = new ConcurrentHashMap<>();
    // sessionId → window start time
    private final Map<String, Long> windowStart = new ConcurrentHashMap<>();
    // active connection count
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        StompCommand command = accessor.getCommand();
        String sessionId = accessor.getSessionId();

        // Track connections
        if (StompCommand.CONNECT.equals(command)) {
            int current = activeConnections.incrementAndGet();
            if (current > MAX_CONCURRENT_CONNECTIONS) {
                activeConnections.decrementAndGet();
                log.warn("[WS] Max concurrent connections reached. Rejecting session {}", sessionId);
                throw new RuntimeException("Server at capacity. Try again later.");
            }
            log.debug("[WS] New connection: {}. Total active: {}", sessionId, current);
        }

        // Release connection slot
        if (StompCommand.DISCONNECT.equals(command)) {
            activeConnections.decrementAndGet();
            messageCounters.remove(sessionId);
            windowStart.remove(sessionId);
            log.debug("[WS] Disconnected: {}. Total active: {}", sessionId, activeConnections.get());
        }

        // Rate limit + size check on SEND
        if (StompCommand.SEND.equals(command)) {

            // 1. Message size check
            byte[] payload = (byte[]) message.getPayload();
            if (payload.length > MAX_MESSAGE_SIZE * 4) { // *4 for UTF-8 bytes
                log.warn("[WS] Oversized message from session {}. Size: {} bytes", sessionId, payload.length);
                throw new RuntimeException("Message too large. Max 500 characters.");
            }

            // 2. Rate limit check — sliding window per session
            long now = System.currentTimeMillis();
            windowStart.putIfAbsent(sessionId, now);
            messageCounters.putIfAbsent(sessionId, new AtomicInteger(0));

            long windowAge = now - windowStart.get(sessionId);
            if (windowAge > 60_000) {
                // Reset window
                windowStart.put(sessionId, now);
                messageCounters.get(sessionId).set(0);
            }

            int count = messageCounters.get(sessionId).incrementAndGet();
            if (count > MAX_MESSAGES_PER_MINUTE) {
                log.warn("[WS] Rate limit exceeded for session {}. Count: {}", sessionId, count);
                throw new RuntimeException("Rate limit exceeded. Max 10 messages per minute.");
            }
        }

        return message;
    }
}