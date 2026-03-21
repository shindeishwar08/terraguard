package com.terraguard.backend.cache;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.terraguard.backend.domain.enums.SignalType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final long CHAT_TTL_HOURS = 24;
    private static final long CHAT_MAX_SIZE = 50;

    // ── Snapshot ────────────────────────────────────────────

    public void saveGlobalSnapshot(String json) {
        try {
            redisTemplate.opsForValue().set(RedisKeys.GLOBAL_SNAPSHOT, json);
        } catch (Exception e) {
            log.error("Failed to save global snapshot to Redis: {}", e.getMessage());
        }
    }

    public String getGlobalSnapshot() {
        try {
            return redisTemplate.opsForValue().get(RedisKeys.GLOBAL_SNAPSHOT);
        } catch (Exception e) {
            log.warn("Redis unavailable when fetching snapshot: {}", e.getMessage());
            return null;
        }
    }

    // ── Dirty Flag ───────────────────────────────────────────

    public void setSnapshotDirty() {
        try {
            redisTemplate.opsForValue().set(RedisKeys.SNAPSHOT_DIRTY, "true");
        } catch (Exception e) {
            log.warn("Could not set snapshot dirty flag: {}", e.getMessage());
        }
    }

    public boolean isSnapshotDirty() {
        try {
            return "true".equals(redisTemplate.opsForValue().get(RedisKeys.SNAPSHOT_DIRTY));
        } catch (Exception e) {
            log.warn("Could not read snapshot dirty flag: {}", e.getMessage());
            return true; // fail-safe: assume dirty, recompile
        }
    }

    public void clearSnapshotDirty() {
        try {
            redisTemplate.delete(RedisKeys.SNAPSHOT_DIRTY);
        } catch (Exception e) {
            log.warn("Could not clear snapshot dirty flag: {}", e.getMessage());
        }
    }

    // ── Signal Tallies (Hash) ────────────────────────────────

    public void incrementSignal(String incidentId, SignalType signalType) {
        try {
            String key = RedisKeys.signalTally(incidentId);
            redisTemplate.opsForHash().increment(key, signalType.name(), 1);
            redisTemplate.expire(key, 15, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("Failed to increment signal [{}/{}]: {}",
                incidentId, signalType, e.getMessage());
        }
    }
    
    public void deleteSignals(String incidentId) {
        try {
            redisTemplate.delete(RedisKeys.signalTally(incidentId));
        } catch (Exception e) {
            log.warn("Could not delete signals for {}: {}", incidentId, e.getMessage());
        }
    }

    public Map<Object, Object> getSignalTally(String incidentId) {
        try {
            return redisTemplate.opsForHash().entries(RedisKeys.signalTally(incidentId));
        } catch (Exception e) {
            log.warn("Redis unavailable when fetching signals for {}: {}", incidentId, e.getMessage());
            return Collections.emptyMap();
        }
    }


    // ── Chat Messages (List) ─────────────────────────────────

    public void appendChatMessage(String incidentId, String messageJson) {
        try {
            String key = RedisKeys.chatMessages(incidentId);
            redisTemplate.opsForList().rightPush(key, messageJson);
            redisTemplate.opsForList().trim(key, -CHAT_MAX_SIZE, -1);
            redisTemplate.expire(key, CHAT_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("Failed to append chat message for incident {}: {}", incidentId, e.getMessage());
        }
    }

    public void deleteChat(String incidentId) {
        try {
            redisTemplate.delete(RedisKeys.chatMessages(incidentId));
        } catch (Exception e) {
            log.warn("Could not delete chat for {}: {}", incidentId, e.getMessage());
        }
    }

    public List<String> getRecentMessages(String incidentId) {
        try {
            List<String> messages = redisTemplate.opsForList()
                    .range(RedisKeys.chatMessages(incidentId), 0, 49);
            return messages != null ? messages : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Redis unavailable when fetching chat for {}: {}", incidentId, e.getMessage());
            return Collections.emptyList();
        }
    }
}