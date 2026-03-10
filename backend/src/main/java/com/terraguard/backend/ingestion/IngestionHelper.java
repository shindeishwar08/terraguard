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
        // Step 1: Upsert raw data
        incidentRepository.upsertIncident(
                title, externalId, source, disasterType,
                magnitude, longitude, latitude
        );

        // Step 2: Fetch hydrated incident
        incidentRepository.findByExternalIdAndSource(
                externalId,
                com.terraguard.backend.domain.enums.DataSource.valueOf(source)
        ).ifPresent(incident -> {

            // Step 3: Capture OLD severity BEFORE recalculation
            BigDecimal oldSeverity = incident.getSeverityIndex();

            // Step 4: Calculate new scores
            BigDecimal newSeverity  = scoringEngineService
                    .calculateSeverityIndex(incident);
            BigDecimal newConfidence = scoringEngineService
                    .calculateConfidenceScore(incident);

            // Step 5: Update in-memory + persist scores
            incident.setSeverityIndex(newSeverity);
            incident.setConfidenceScore(newConfidence);
            incidentRepository.updateScores(
                    incident.getId(), newSeverity, newConfidence);

            // Step 6: FSM evaluation with true delta
            lifecycleService.evaluateTransition(incident, oldSeverity);

            // Step 7: Invalidate snapshot (AFTER all DB writes are done)
            cacheService.setSnapshotDirty();

            log.debug("[INGEST] {} scored: severity={} confidence={} delta={}",
                    externalId, newSeverity, newConfidence,
                    newSeverity.subtract(oldSeverity));
        });
    }
}