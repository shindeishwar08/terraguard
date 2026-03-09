package com.terraguard.backend.ingestion;

import com.terraguard.backend.cache.CacheService;
import com.terraguard.backend.domain.entity.IncidentRepository;
import com.terraguard.backend.lifecycle.IncidentLifecycleService;
import com.terraguard.backend.scoring.ScoringEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionHelper {

    private final IncidentRepository incidentRepository;
    private final CacheService cacheService;
    private final IncidentLifecycleService lifecycleService;
    private final ScoringEngineService scoringEngineService;

    public void persistIncident(
            String title,
            String externalId,
            String source,
            String disasterType,
            Double magnitude,
            double longitude,
            double latitude
    ) {
        // Step 1: Upsert — also updates contributing_sources
        incidentRepository.upsertIncident(
                title, externalId, source, disasterType,
                magnitude, longitude, latitude
        );

        // Step 2: Fetch saved incident
        incidentRepository.findByExternalIdAndSource(
                externalId,
                com.terraguard.backend.domain.enums.DataSource.valueOf(source)
        ).ifPresent(incident -> {

            // Step 3: Calculate scores
            BigDecimal severity   = scoringEngineService.calculateSeverityIndex(incident);
            BigDecimal confidence = scoringEngineService.calculateConfidenceScore(incident);

            // FIX: Update the Java object in memory so the FSM sees the real numbers!
            incident.setSeverityIndex(severity);
            incident.setConfidenceScore(confidence);

            // Step 4: Save scores back to DB
            incidentRepository.updateScores(incident.getId(), severity, confidence);

            // Step 5: Invalidate snapshot cache
            cacheService.setSnapshotDirty();

            // Step 6: Evaluate FSM transition (Now it evaluates the correct scores!)
            lifecycleService.evaluateTransition(incident);

            log.debug("[INGEST] Scored incident {}: severity={}, confidence={}",
                    externalId, severity, confidence);
        });
    }
}