package com.terraguard.backend.ingestion.nasa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraguard.backend.domain.enums.DataSource;
import com.terraguard.backend.ingestion.IngestionHelper;
import com.terraguard.backend.ingestion.nasa.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class NasaEonetIngestionService {

    private final WebClient webClient;
    private final IngestionHelper ingestionHelper;
    private final ObjectMapper objectMapper;

    private static final String NASA_URL =
        "https://eonet.gsfc.nasa.gov/api/v3/events?status=open&limit=50";
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    private static final Map<String, String> CATEGORY_MAP = Map.of(
        "wildfires",    "WILDFIRE",
        "severeStorms", "CYCLONE",
        "floods",       "FLOOD",
        "earthquakes",  "EARTHQUAKE"
    );

    @Scheduled(fixedRate = 300000)
    public void ingestEvents() {
        if (consecutiveFailures.get() >= MAX_CONSECUTIVE_FAILURES) {
            log.warn("[NASA] Circuit breaker OPEN — skipping poll");
            return;
        }

        try {
            String rawJson = webClient.get()
                .uri(NASA_URL)
                .header("Accept", "application/json")
                .header("User-Agent", "TerraGuard/1.0")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (rawJson == null || rawJson.isBlank()) {
                consecutiveFailures.incrementAndGet();
                return;
            }

            EonetEventCollection response = objectMapper.readValue(
                rawJson, EonetEventCollection.class);

            if (response.getEvents() == null) {
                consecutiveFailures.incrementAndGet();
                return;
            }

            consecutiveFailures.set(0);
            log.info("[NASA] Fetched {} events", response.getEvents().size());

            for (EonetEvent event : response.getEvents()) {
                processEvent(event);
            }

        } catch (Exception e) {
            int failures = consecutiveFailures.incrementAndGet();
            log.error("[NASA] Poll failed ({}/{}): {}",
                    failures, MAX_CONSECUTIVE_FAILURES, e.getMessage());
        }
    }

    private void processEvent(EonetEvent event) {
        try {
            String disasterType = resolveDisasterType(event);
            if (disasterType == null) {
                log.debug("[NASA] Skipping unsupported category for: {}", event.getId());
                return;
            }

            double[] coords = resolveCoordinates(event);
            if (coords == null) {
                log.warn("[NASA] No usable geometry for: {}", event.getId());
                return;
            }

            Double magnitudeValue = event.getGeometry().isEmpty() ? null :
                    event.getGeometry()
                         .get(event.getGeometry().size() - 1)
                         .getMagnitudeValue();
            if (magnitudeValue != null && magnitudeValue >= 1000.0) {
                magnitudeValue = 999.99; // Cap it to max allowed DB size
            }

            ingestionHelper.persistIncident(
                    event.getTitle(),
                    event.getId(),
                    DataSource.NASA_EONET.name(),
                    disasterType,
                    magnitudeValue,
                    coords[0],
                    coords[1]
            );

        } catch (Exception e) {
            log.error("[NASA] Failed to process event {}: {}",
                    event.getId(), e.getMessage());
        }
    }

    private String resolveDisasterType(EonetEvent event) {
        if (event.getCategories() == null) return null;
        return event.getCategories().stream()
                .map(c -> CATEGORY_MAP.get(c.getId()))
                .filter(type -> type != null)
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private double[] resolveCoordinates(EonetEvent event) {
        if (event.getGeometry() == null || event.getGeometry().isEmpty()) return null;

        EonetGeometry latest = event.getGeometry()
                .get(event.getGeometry().size() - 1);

        if ("Point".equals(latest.getType())) {
            List<Double> coords = (List<Double>) latest.getCoordinates();
            return new double[]{coords.get(0), coords.get(1)};

        } else if ("Polygon".equals(latest.getType())) {
            List<List<List<Double>>> rings =
                (List<List<List<Double>>>) latest.getCoordinates();
            return calculateCentroid(rings.get(0));
        }

        return null;
    }

    private double[] calculateCentroid(List<List<Double>> ring) {
        double sumLon = 0, sumLat = 0;
        for (List<Double> point : ring) {
            sumLon += point.get(0);
            sumLat += point.get(1);
        }
        return new double[]{sumLon / ring.size(), sumLat / ring.size()};
    }
}