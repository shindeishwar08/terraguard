package com.terraguard.backend.domain.enums;

import java.util.Set;

public enum IncidentStatus {

    DETECTED(Set.of("CONFIRMED")),
    CONFIRMED(Set.of("ESCALATING", "STABLE")),
    ESCALATING(Set.of("STABLE")),
    STABLE(Set.of("ESCALATING", "RESOLVED")),
    RESOLVED(Set.of("ARCHIVED")),
    ARCHIVED(Set.of());

    private final Set<String> allowedTransitions;

    IncidentStatus(Set<String> allowedTransitions) {
        this.allowedTransitions = allowedTransitions;
    }

    public boolean canTransitionTo(IncidentStatus next) {
        return this.allowedTransitions.contains(next.name());
    }
}