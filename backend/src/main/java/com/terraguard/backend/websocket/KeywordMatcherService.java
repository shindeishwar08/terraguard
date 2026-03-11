package com.terraguard.backend.websocket;

import com.terraguard.backend.config.HighlightConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordMatcherService {

    private final HighlightConfig highlightConfig;

    public boolean isHighlightWorthy(String message) {
        if (message == null || message.isBlank()) return false;

        String normalized = message.toLowerCase();
        List<String> keywords = highlightConfig.getKeywords();

        if (keywords == null || keywords.isEmpty()) {
            log.warn("[KEYWORD] No keywords configured — all messages will be non-highlighted");
            return false;
        }

        return keywords.stream()
                .anyMatch(keyword -> normalized.contains(keyword));
    }
}