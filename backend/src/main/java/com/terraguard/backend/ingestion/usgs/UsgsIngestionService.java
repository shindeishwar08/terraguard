package com.terraguard.backend.ingestion.usgs;

import com.terraguard.backend.cache.CacheService;
import com.terraguard.backend.domain.entity.IncidentRepository;
import com.terraguard.backend.domain.enums.DataSource;
import com.terraguard.backend.ingestion.usgs.dto.UsgsFeature;
import com.terraguard.backend.ingestion.usgs.dto.UsgsFeatureCollection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsgsIngestionService {

    private final WebClient webClient;
    private final IncidentRepository incidentRepository;
    private final CacheService cacheService;

    private static final String USGS_URL =
        "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/significant_month.geojson";
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    // AtomicInteger — thread-safe counter (scheduler can run on different threads)
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    @Scheduled(fixedRate = 60000)
    public void ingestEarthquakes() {

        // ── Circuit Breaker Check ────────────────────────────
        if (consecutiveFailures.get() >= MAX_CONSECUTIVE_FAILURES) {
            log.warn("[USGS] Circuit breaker OPEN — skipping poll. " +
                     "Consecutive failures: {}", consecutiveFailures.get());
            return;
        }

        log.info("[USGS] Starting earthquake ingestion poll");

        try {
            // ── Step 1: FETCH ────────────────────────────────
            UsgsFeatureCollection response = webClient.get()
                    .uri(USGS_URL)
                    .retrieve()
                    .bodyToMono(UsgsFeatureCollection.class)
                    .block(); // blocking — acceptable for scheduled background task

            if (response == null || response.getFeatures() == null) {
                log.warn("[USGS] Empty response received");
                consecutiveFailures.incrementAndGet();
                return;
            }

            // ── Reset circuit breaker on success ─────────────
            consecutiveFailures.set(0);
            log.info("[USGS] Fetched {} earthquakes", response.getFeatures().size());

            // ── Step 2 & 3: PARSE + UPSERT each feature ──────
            for (UsgsFeature feature : response.getFeatures()) {
                processFeature(feature);
            }

        } catch (Exception e) {
            int failures = consecutiveFailures.incrementAndGet();
            log.error("[USGS] Poll failed ({}/{}): {}",
                    failures, MAX_CONSECUTIVE_FAILURES, e.getMessage());
        }
    }

    private void processFeature(UsgsFeature feature) {
        try {
            // ── Step 2: PARSE + NORMALIZE ────────────────────
            if (feature.getGeometry() == null ||
                feature.getGeometry().getCoordinates() == null) {
                log.warn("[USGS] Skipping feature {} — no geometry", feature.getId());
                return;
            }

            String externalId = feature.getId();
            Double magnitude  = feature.getProperties().getMag();
            String title      = feature.getProperties().getTitle();
            double longitude  = feature.getGeometry().getLongitude();
            double latitude   = feature.getGeometry().getLatitude();

            // ── Step 3: UPSERT ───────────────────────────────
            incidentRepository.upsertIncident(
                    title,
                    externalId,
                    DataSource.USGS.name(),
                    "EARTHQUAKE",
                    magnitude,
                    longitude,
                    latitude
            );

            // ── Step 4: Mark snapshot dirty ──────────────────
            cacheService.setSnapshotDirty();

            log.debug("[USGS] Upserted: {} ({})", externalId, title);

        } catch (Exception e) {
            // Per-feature isolation — one bad earthquake doesn't stop others
            log.error("[USGS] Failed to process feature {}: {}",
                    feature.getId(), e.getMessage());
        }
    }
}