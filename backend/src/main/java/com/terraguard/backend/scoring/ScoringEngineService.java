package com.terraguard.backend.scoring;

import com.terraguard.backend.cache.CacheService;
import com.terraguard.backend.domain.entity.Incident;
import com.terraguard.backend.domain.enums.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringEngineService {

    private final CacheService cacheService;

    // Confidence weights per source
    private static final Map<String, Integer> SOURCE_WEIGHTS = Map.of(
        DataSource.USGS.name(),       40,
        DataSource.NASA_EONET.name(), 20,
        DataSource.GDACS.name(),      20
    );

    private static final int CROWD_SIGNAL_CAP = 10;

    // ── Severity ─────────────────────────────────────────────

    public BigDecimal calculateSeverityIndex(Incident incident) {
        BigDecimal severity = switch (incident.getDisasterType()) {
            case EARTHQUAKE -> calculateEarthquakeSeverity(incident);
            case WILDFIRE   -> calculateWildfireSeverity(incident);
            case FLOOD,
                 CYCLONE   -> calculateFloodCycloneSeverity(incident);
            default -> {
                log.warn("[SCORING] Unknown disaster type: {}", incident.getDisasterType());
                yield BigDecimal.ZERO;
            }
        };

        // Hard cap 0-100
        return severity
                .max(BigDecimal.ZERO)
                .min(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateEarthquakeSeverity(Incident incident) {
        if (incident.getMagnitude() == null) return BigDecimal.ZERO;

        double magnitude = incident.getMagnitude().doubleValue();
        double score = magnitude * 10;

        // Depth stored in magnitude for USGS — we approximate from title
        // Real depth scoring will come from raw data enrichment in Phase 3
        return BigDecimal.valueOf(score);
    }

    private BigDecimal calculateWildfireSeverity(Incident incident) {
        if (incident.getMagnitude() == null) return BigDecimal.valueOf(15);

        double areaNormalized = Math.min(
            incident.getMagnitude().doubleValue() / 10.0, 85.0);
        return BigDecimal.valueOf(areaNormalized + 15); // active bonus
    }

    private BigDecimal calculateFloodCycloneSeverity(Incident incident) {
        if (incident.getMagnitude() == null) return BigDecimal.ZERO;
        // magnitude already stores pre-converted severity (25/55/85)
        return incident.getMagnitude();
    }

    // ── Confidence ───────────────────────────────────────────

    public BigDecimal calculateConfidenceScore(Incident incident) {
        String contributing = incident.getContributingSources();
        int score = 0;

        // Add weight for each contributing source
        for (Map.Entry<String, Integer> entry : SOURCE_WEIGHTS.entrySet()) {
            if (contributing.contains("," + entry.getKey() + ",")) {
                score += entry.getValue();
            }
        }

        // Crowd signal contribution — capped at +10
        int crowdBonus = calculateCrowdBonus(incident);
        score += crowdBonus;

        return BigDecimal.valueOf(Math.min(score, 100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private int calculateCrowdBonus(Incident incident) {
        try {
            Map<Object, Object> signals = cacheService
                    .getSignalTally(incident.getId().toString());
            if (signals == null || signals.isEmpty()) return 0;

            int totalSignals = signals.values().stream()
                    .mapToInt(v -> Integer.parseInt(v.toString()))
                    .sum();

            // Every 10 signals = +1 confidence, capped at +10
            return Math.min(totalSignals / 10, CROWD_SIGNAL_CAP);
        } catch (Exception e) {
            log.warn("[SCORING] Could not fetch crowd signals for {}: {}",
                    incident.getId(), e.getMessage());
            return 0;
        }
    }
}