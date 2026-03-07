package com.terraguard.backend.ingestion;

import com.terraguard.backend.cache.CacheService;
import com.terraguard.backend.domain.entity.IncidentRepository;
import com.terraguard.backend.lifecycle.IncidentLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionHelper {

    private final IncidentRepository incidentRepository;
    private final CacheService cacheService;
    private final IncidentLifecycleService lifecycleService;

    public void persistIncident(
            String title,
            String externalId,
            String source,
            String disasterType,
            Double magnitude,
            double longitude,
            double latitude
    ) {
        // Step 1: Upsert to Postgres
        incidentRepository.upsertIncident(
                title, externalId, source, disasterType,
                magnitude, longitude, latitude
        );

        // Step 2: Invalidate snapshot cache
        cacheService.setSnapshotDirty();

        // Step 3: Fetch saved incident and evaluate FSM transition
        incidentRepository.findByExternalIdAndSource(
                externalId,
                com.terraguard.backend.domain.enums.DataSource
                        .valueOf(source)
        ).ifPresent(lifecycleService::evaluateTransition);

        log.debug("[INGEST] Persisted: {} from {}", externalId, source);
    }
}