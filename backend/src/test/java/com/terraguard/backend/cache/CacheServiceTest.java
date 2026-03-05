package com.terraguard.backend.cache;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CacheServiceTest {

    @Autowired
    private CacheService cacheService;

    private static final String TEST_INCIDENT_ID = "test-incident-001";

    @BeforeEach
    void cleanup() {
        // Clean state before each test
        cacheService.clearSnapshotDirty();
         // Clean test incident data too
        cacheService.deleteChat(TEST_INCIDENT_ID);
        cacheService.deleteSignals(TEST_INCIDENT_ID);
    }

    // ── Snapshot Tests ───────────────────────────────────

    @Test
    void shouldSaveAndRetrieveSnapshot() {
        // Arrange
        String json = "[{\"id\":\"abc\",\"title\":\"Test Earthquake\"}]";

        // Act
        cacheService.saveGlobalSnapshot(json);
        String retrieved = cacheService.getGlobalSnapshot();

        // Assert
        assertEquals(json, retrieved);
    }

    // ── Dirty Flag Tests ─────────────────────────────────

    @Test
    void shouldSetAndClearDirtyFlag() {
        cacheService.setSnapshotDirty();
        assertTrue(cacheService.isSnapshotDirty());

        cacheService.clearSnapshotDirty();
        assertFalse(cacheService.isSnapshotDirty());
    }

    // ── Signal Tally Tests ───────────────────────────────

    @Test
    void shouldIncrementSignalTally() {
        // Act — increment same signal 5 times
        for (int i = 0; i < 5; i++) {
            cacheService.incrementSignal(TEST_INCIDENT_ID,
                com.terraguard.backend.domain.enums.SignalType.ROAD_BLOCKED);
        }

        // Assert
        Map<Object, Object> tally = cacheService.getSignalTally(TEST_INCIDENT_ID);
        assertEquals("5", tally.get("ROAD_BLOCKED").toString());
    }

    // ── Chat Message Tests ───────────────────────────────

    @Test
    void shouldCapChatMessagesAt50() {
        // Act — push 80 messages
        for (int i = 0; i < 80; i++) {
            cacheService.appendChatMessage(TEST_INCIDENT_ID, "message-" + i);
        }

        // Assert — only last 50 remain
        List<String> messages = cacheService.getRecentMessages(TEST_INCIDENT_ID);
        assertEquals(50, messages.size());
    }

    @Test
    void shouldReturnNewestMessagesAfterTrim() {
        // Push 60 messages
        for (int i = 0; i < 60; i++) {
            cacheService.appendChatMessage(TEST_INCIDENT_ID, "message-" + i);
        }

        // After trim, oldest 10 are gone. First message should be message-10
        List<String> messages = cacheService.getRecentMessages(TEST_INCIDENT_ID);
        assertEquals("message-10", messages.get(0));
        assertEquals("message-59", messages.get(49));
    }
}