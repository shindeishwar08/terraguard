package com.terraguard.backend.domain.enums;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class IncidentStatusTest {

    @Test
    void shouldAllowDetectedToConfirmed() {
        assertTrue(IncidentStatus.DETECTED.canTransitionTo(IncidentStatus.CONFIRMED));
    }

    @Test
    void shouldNotAllowResolvedToDetected() {
        assertFalse(IncidentStatus.RESOLVED.canTransitionTo(IncidentStatus.DETECTED));
    }

    @Test
    void shouldAllowStableToEscalating() {
        // Nature re-intensifies — this must be allowed
        assertTrue(IncidentStatus.STABLE.canTransitionTo(IncidentStatus.ESCALATING));
    }

    @Test
    void shouldNotAllowArchivedToAnything() {
        // Terminal state — no transitions out
        assertFalse(IncidentStatus.ARCHIVED.canTransitionTo(IncidentStatus.DETECTED));
        assertFalse(IncidentStatus.ARCHIVED.canTransitionTo(IncidentStatus.CONFIRMED));
        assertFalse(IncidentStatus.ARCHIVED.canTransitionTo(IncidentStatus.RESOLVED));
    }
}