package com.terraguard.backend.lifecycle;

import com.terraguard.backend.cache.CacheService;
import com.terraguard.backend.domain.entity.Incident;
import com.terraguard.backend.domain.entity.IncidentRepository;
import com.terraguard.backend.domain.entity.IncidentTimeline;
import com.terraguard.backend.domain.entity.IncidentTimelineRepository;
import com.terraguard.backend.domain.enums.IncidentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentLifecycleService {

    private final IncidentRepository incidentRepository;
    private final IncidentTimelineRepository timelineRepository;
    private final CacheService cacheService;

    private static final BigDecimal ESCALATION_THRESHOLD = BigDecimal.valueOf(70);
    private static final BigDecimal STABLE_THRESHOLD     = BigDecimal.valueOf(50);
    private static final BigDecimal SEVERITY_DELTA       = BigDecimal.valueOf(10);
    private static final double     CONFIRMATION_RADIUS  = 50000.0;
    private static final long       QUIET_HOURS          = 2;
    private static final int        CROWD_SURGE_THRESHOLD = 10;

    @Transactional
    public void evaluateTransition(Incident incident, BigDecimal oldSeverity) {

        // ── GUARD ────────────────────────────────────────────
        if (Boolean.TRUE.equals(incident.getOverrideLocked())) {
            log.warn("[FSM] Incident {} is override_locked — skipping",
                    incident.getExternalId());
            return;
        }

        IncidentStatus current = incident.getStatus();
        if (current == null) return;

        // ── EVALUATE ─────────────────────────────────────────
        switch (current) {
            case DETECTED   -> evaluateDetected(incident);
            case CONFIRMED  -> evaluateConfirmed(incident, oldSeverity);
            case ESCALATING -> evaluateEscalating(incident);
            case STABLE     -> evaluateStable(incident, oldSeverity);
            default -> log.debug("[FSM] No transitions for status: {}", current);
        }
    }

    // ── DETECTED → CONFIRMED ─────────────────────────────────

    private void evaluateDetected(Incident incident) {
        boolean secondSourceNearby = incidentRepository
                .existsNearbyFromDifferentSource(
                        incident.getId(),
                        incident.getSource().name(),
                        CONFIRMATION_RADIUS
                );

        if (secondSourceNearby) {
            transition(incident, IncidentStatus.CONFIRMED,
                    "Second independent source confirmed within 50km radius");
        }
    }

    // ── CONFIRMED → ESCALATING or STABLE ─────────────────────

    private void evaluateConfirmed(Incident incident, BigDecimal oldSeverity) {
        BigDecimal newSeverity = incident.getSeverityIndex();
        BigDecimal delta = newSeverity.subtract(oldSeverity);

        // Escalation conditions
        if (delta.compareTo(SEVERITY_DELTA) > 0) {
            transition(incident, IncidentStatus.ESCALATING,
                    "Severity delta " + delta + " exceeded threshold of 10");
            return;
        }

        if (newSeverity.compareTo(ESCALATION_THRESHOLD) > 0) {
            transition(incident, IncidentStatus.ESCALATING,
                    "Severity index " + newSeverity + " exceeded escalation threshold");
            return;
        }

        int crowdSignals = getTotalCrowdSignals(incident.getId());
        if (crowdSignals > CROWD_SURGE_THRESHOLD) {
            transition(incident, IncidentStatus.ESCALATING,
                    "Crowd signal surge: " + crowdSignals + " signals reported");
            return;
        }

        // Stable condition
        boolean quietFor2Hours = incident.getUpdatedAt()
                .isBefore(OffsetDateTime.now().minusHours(QUIET_HOURS));

        if (quietFor2Hours && newSeverity.compareTo(STABLE_THRESHOLD) < 0) {
            transition(incident, IncidentStatus.STABLE,
                    "Severity stable below 50 for 2 hours — no escalation triggers");
        }
    }

    // ── ESCALATING → STABLE ───────────────────────────────────

    private void evaluateEscalating(Incident incident) {
        long hoursSinceUpdate = ChronoUnit.HOURS.between(
                incident.getUpdatedAt(), OffsetDateTime.now());

        if (hoursSinceUpdate >= QUIET_HOURS) {
            transition(incident, IncidentStatus.STABLE,
                    "No escalation triggers for " + hoursSinceUpdate + " hours");
        }
    }

    // ── STABLE → ESCALATING or RESOLVED ──────────────────────

    private void evaluateStable(Incident incident, BigDecimal oldSeverity) {
        BigDecimal newSeverity = incident.getSeverityIndex();
        BigDecimal delta = newSeverity.subtract(oldSeverity);

        // Re-intensification
        if (delta.compareTo(SEVERITY_DELTA) > 0) {
            transition(incident, IncidentStatus.ESCALATING,
                    "Severity re-escalated — delta: " + delta);
            return;
        }

        // Time-based resolved
        boolean quietFor2Hours = incident.getUpdatedAt()
                .isBefore(OffsetDateTime.now().minusHours(QUIET_HOURS));

        if (quietFor2Hours && newSeverity.compareTo(STABLE_THRESHOLD) < 0) {
            transition(incident, IncidentStatus.RESOLVED,
                    "No activity for 2+ hours with severity below 50");
        }
    }

    // ── STALE INCIDENT PURGE JOB ──────────────────────────────
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void evaluateStaleIncidents() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusHours(QUIET_HOURS);
        
        List<Incident> staleIncidents = incidentRepository.findStaleIncidents(
            List.of(IncidentStatus.ESCALATING, IncidentStatus.CONFIRMED),
            cutoff
        );

        if (staleIncidents.isEmpty()) return;

        log.info("[FSM] Evaluating {} stale incidents", staleIncidents.size());

        for (Incident incident : staleIncidents) {
            try {
                evaluateTransition(incident, incident.getSeverityIndex());
            } catch (Exception e) {
                log.error("[FSM] Failed to evaluate stale incident {}: {}",
                    incident.getExternalId(), e.getMessage());
                }
            }
        }
    

    // ── forceResolve — NASA agency closed flag ────────────────

    @Transactional
    public void forceResolve(Incident incident, String source) {
        if (Boolean.TRUE.equals(incident.getOverrideLocked())) {
            log.warn("[FSM] forceResolve blocked — incident {} is override_locked",
                    incident.getExternalId());
            return;
        }

        transition(incident, IncidentStatus.RESOLVED,
                "Agency closed flag received from " + source);
    }

    // ── Transition executor ───────────────────────────────────

    private void transition(Incident incident,
                            IncidentStatus next,
                            String reason) {
        IncidentStatus previous = incident.getStatus();

        // VALIDATE — FSM contract
        if (!previous.canTransitionTo(next)) {
            log.warn("[FSM] Invalid transition {} → {} for incident {}",
                    previous, next, incident.getExternalId());
            return;
        }

        // Update status in DB
        incidentRepository.updateStatus(incident.getId(), next.name());

        // IncidentTimeline entry = new IncidentTimeline();
        // entry.setIncident(incident);
        // entry.setPreviousStatus(previous.name());
        // entry.setNewStatus(next.name());
        // entry.setMessage(reason);
        // timelineRepository.save(entry);


        // AUDIT — append timeline entry
        IncidentTimeline entry = IncidentTimeline.builder()
        .incident(incident)
        .previousStatus(previous.name())
        .newStatus(next.name())
        .message(reason)
        .build();

        timelineRepository.save(entry);

        // Invalidate snapshot cache
        cacheService.setSnapshotDirty();

        log.info("[FSM] {} → {} | incident={} | reason={}",
                previous, next, incident.getExternalId(), reason);
    }

    // ── Helpers ───────────────────────────────────────────────

    private int getTotalCrowdSignals(UUID incidentId) {
        try {
            Map<Object, Object> signals = cacheService
                    .getSignalTally(incidentId.toString());
            if (signals == null || signals.isEmpty()) return 0;
            return signals.values().stream()
                    .mapToInt(v -> Integer.parseInt(v.toString()))
                    .sum();
        } catch (Exception e) {
            log.warn("[FSM] Could not fetch crowd signals for {}: {}",
                    incidentId, e.getMessage());
            return 0;
        }
    }
}