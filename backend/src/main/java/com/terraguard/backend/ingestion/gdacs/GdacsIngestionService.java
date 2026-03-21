package com.terraguard.backend.ingestion.gdacs;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.terraguard.backend.domain.enums.DataSource;
import com.terraguard.backend.ingestion.IngestionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Element;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class GdacsIngestionService {

    private final WebClient webClient;
    private final IngestionHelper ingestionHelper;

    private static final String GDACS_URL = "https://www.gdacs.org/xml/rss.xml";
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    private static final Map<String, String> EVENT_TYPE_MAP = Map.of(
        "EQ", "EARTHQUAKE",
        "WF", "WILDFIRE",
        "FL", "FLOOD",
        "TC", "CYCLONE"
    );

    private static final Map<String, Double> ALERT_SEVERITY_MAP = Map.of(
        "Green",  25.0,
        "Orange", 55.0,
        "Red",    85.0
    );

    @Scheduled(fixedRate = 300000)
    public void ingestEvents() {
        if (consecutiveFailures.get() >= MAX_CONSECUTIVE_FAILURES) {
            log.warn("[GDACS] Circuit breaker OPEN — skipping poll");
            return;
        }

        try {
            String xml = webClient.get()
                    .uri(GDACS_URL)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (xml == null || xml.isBlank()) {
                consecutiveFailures.incrementAndGet();
                return;
            }

            String cleanXml = xml.trim().replaceFirst("^\uFEFF", "");
            SyndFeed feed = new SyndFeedInput().build(new StringReader(cleanXml));

            consecutiveFailures.set(0);
            log.info("[GDACS] Fetched {} entries", feed.getEntries().size());

            for (SyndEntry entry : feed.getEntries()) {
                processEntry(entry);
            }

        } catch (Exception e) {
            int failures = consecutiveFailures.incrementAndGet();
            log.error("[GDACS] Poll failed ({}/{}): {}",
                    failures, MAX_CONSECUTIVE_FAILURES, e.getMessage());
        }
    }
    private void processEntry(SyndEntry entry) {
    try {
        List<Element> foreignMarkup = (List<Element>) entry.getForeignMarkup();

        String eventId    = getElement(foreignMarkup, "eventid",   "gdacs");
        String eventType  = getElement(foreignMarkup, "eventtype", "gdacs");
        String alertLevel = getElement(foreignMarkup, "alertlevel","gdacs");
        String georssPoint= getElement(foreignMarkup, "point",     "georss");

        if (eventId == null || eventType == null || georssPoint == null) {
            log.warn("[GDACS] Missing required fields, skipping entry");
            return;
        }

        String disasterType = EVENT_TYPE_MAP.get(eventType);
        if (disasterType == null) {
            log.debug("[GDACS] Skipping unsupported type: {}", eventType);
            return;
        }
        // Filter minor wildfires — Green alert level not significant
        if ("WILDFIRE".equals(disasterType) && ALERT_SEVERITY_MAP.getOrDefault(alertLevel, 25.0) <= 25.0) {
            log.debug("[GDACS] Skipping green wildfire {}", eventId);
            return;
        }

        // georss:point = "lat lon" — note: lat first here, opposite of GeoJSON
        String[] parts   = georssPoint.trim().split("\\s+");
        double latitude  = Double.parseDouble(parts[0]);
        double longitude = Double.parseDouble(parts[1]);

        // Construct unique externalId: EQ1527754
        String externalId = eventType + eventId;

        // Double severity = ALERT_SEVERITY_MAP.getOrDefault(alertLevel, 25.0);

        // ingestionHelper.persistIncident(
        //         entry.getTitle(),
        //         externalId,
        //         DataSource.GDACS.name(),
        //         disasterType,
        //         severity,
        //         longitude,
        //         latitude
        // );
        // Extract real magnitude only for EQ and TC
        Double realMagnitude = null;
        if ("EARTHQUAKE".equals(disasterType) || "CYCLONE".equals(disasterType)) {
            Element severityEl = foreignMarkup.stream()
                .filter(e -> "severity".equals(e.getName())
                        && "gdacs".equals(e.getNamespacePrefix()))
                .findFirst().orElse(null);
            if (severityEl != null) {
                String val = severityEl.getAttributeValue("value");
                if (val != null) {
                    try { realMagnitude = Double.parseDouble(val); }
                    catch (Exception ignored) {}
                }
            }
        }

        // For FLOOD — use alert score as magnitude (no real value available)
        if (realMagnitude == null) {
            realMagnitude = ALERT_SEVERITY_MAP.getOrDefault(alertLevel, 25.0);
        }

        // Use realMagnitude for storage, alertSeverity kept for reference
        ingestionHelper.persistIncident(
            entry.getTitle(),
            externalId,
            DataSource.GDACS.name(),
            disasterType,
            realMagnitude,  // ← real magnitude now
            longitude,
            latitude
        );

    } catch (Exception e) {
        log.error("[GDACS] Failed to process entry: {}", e.getMessage());
    }
}

    private String getElement(List<Element> elements, String name, String prefix) {
        if (elements == null) return null;
        return elements.stream()
                .filter(e -> name.equals(e.getName()) &&
                             prefix.equals(e.getNamespacePrefix()))
                .map(Element::getValue)
                .findFirst()
                .orElse(null);
    }
}