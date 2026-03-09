package com.terraguard.backend.scoring;

import com.terraguard.backend.cache.CacheService;
import com.terraguard.backend.domain.entity.Incident;
import com.terraguard.backend.domain.enums.DisasterType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ScoringEngineServiceTest {

    @Mock
    private CacheService cacheService;

    private ScoringEngineService scoringEngine;

    @BeforeEach
    void setup() {
        scoringEngine = new ScoringEngineService(cacheService);
        // Default — no crowd signals
        when(cacheService.getSignalTally(any()))
                .thenReturn(Collections.emptyMap());
    }

    // ── Earthquake Severity ──────────────────────────────────

   @Test
    void earthquake_magnitude9_shouldBe90() {
    Incident incident = buildIncident(DisasterType.EARTHQUAKE, 9.0, ",USGS,");
    BigDecimal result = scoringEngine.calculateSeverityIndex(incident);
    assertEquals(new BigDecimal("90.00"), result);
    }

    @Test
    void earthquake_magnitude10_shouldBeCappedAt100() {
    Incident incident = buildIncident(DisasterType.EARTHQUAKE, 10.0, ",USGS,");
    BigDecimal result = scoringEngine.calculateSeverityIndex(incident);
    assertEquals(new BigDecimal("100.00"), result);
    }
    @Test
    void earthquake_magnitude4_shouldBe40() {
        Incident incident = buildIncident(DisasterType.EARTHQUAKE, 4.0, ",USGS,");
        BigDecimal result = scoringEngine.calculateSeverityIndex(incident);
        assertEquals(new BigDecimal("40.00"), result);
    }

    @Test
    void earthquake_nullMagnitude_shouldBe0() {
        Incident incident = buildIncident(DisasterType.EARTHQUAKE, null, ",USGS,");
        BigDecimal result = scoringEngine.calculateSeverityIndex(incident);
        assertEquals(new BigDecimal("0.00"), result);
    }

    // ── Wildfire Severity ────────────────────────────────────

    @Test
    void wildfire_nullMagnitude_shouldReturnActiveBonusOnly() {
        Incident incident = buildIncident(DisasterType.WILDFIRE, null, ",NASA_EONET,");
        BigDecimal result = scoringEngine.calculateSeverityIndex(incident);
        assertEquals(new BigDecimal("15.00"), result);
    }

    // ── Flood/Cyclone Severity ───────────────────────────────

    @Test
    void flood_greenAlert_shouldBe25() {
        Incident incident = buildIncident(DisasterType.FLOOD, 25.0, ",GDACS,");
        BigDecimal result = scoringEngine.calculateSeverityIndex(incident);
        assertEquals(new BigDecimal("25.00"), result);
    }

    @Test
    void cyclone_redAlert_shouldBe85() {
        Incident incident = buildIncident(DisasterType.CYCLONE, 85.0, ",GDACS,");
        BigDecimal result = scoringEngine.calculateSeverityIndex(incident);
        assertEquals(new BigDecimal("85.00"), result);
    }

    // ── Confidence Score ─────────────────────────────────────

    @Test
    void confidence_usgsOnly_shouldBe40() {
        Incident incident = buildIncident(DisasterType.EARTHQUAKE, 6.0, ",USGS,");
        BigDecimal result = scoringEngine.calculateConfidenceScore(incident);
        assertEquals(new BigDecimal("40.00"), result);
    }

    @Test
    void confidence_usgsAndNasa_shouldBe60() {
        Incident incident = buildIncident(DisasterType.EARTHQUAKE, 6.0, ",USGS,NASA_EONET,");
        BigDecimal result = scoringEngine.calculateConfidenceScore(incident);
        assertEquals(new BigDecimal("60.00"), result);
    }

    @Test
    void confidence_sameSourceTwice_shouldNotDoubleCount() {
        Incident incident = buildIncident(DisasterType.EARTHQUAKE, 6.0, ",USGS,USGS,");
        BigDecimal result = scoringEngine.calculateConfidenceScore(incident);
        assertEquals(new BigDecimal("40.00"), result);
    }

    @Test
    void confidence_allThreeSources_shouldBe80() {
        Incident incident = buildIncident(DisasterType.EARTHQUAKE, 6.0,
                ",USGS,NASA_EONET,GDACS,");
        BigDecimal result = scoringEngine.calculateConfidenceScore(incident);
        assertEquals(new BigDecimal("80.00"), result);
    }

    // ── Helper ───────────────────────────────────────────────

    private Incident buildIncident(DisasterType type, Double magnitude,
                                    String contributingSources) {
        Incident incident = new Incident();
        incident.setId(UUID.randomUUID()); // Prevents NullPointerException!
        incident.setDisasterType(type);
        incident.setMagnitude(magnitude != null ?
                BigDecimal.valueOf(magnitude) : null);
        incident.setContributingSources(contributingSources);
        return incident;
    }
}