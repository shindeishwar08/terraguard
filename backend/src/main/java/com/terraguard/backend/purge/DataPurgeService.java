package com.terraguard.backend.purge;

import java.time.OffsetDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.terraguard.backend.domain.entity.IncidentRepository;
import com.terraguard.backend.domain.enums.IncidentStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataPurgeService {
    
    private final IncidentRepository incidentRepository;

    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    public void purgeArchivedIncidents(){
        try {
            OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(15);

            int deletedEntries = incidentRepository.deleteByStatusAndUpdatedAtBefore(
                IncidentStatus.ARCHIVED, 
                cutoffDate
            );

            log.info("[PURGE] Successfully deleted {} old archived incidents.", deletedEntries);
            
        } catch (Exception e) {
            log.error("[PURGE] Nightly data purge failed to execute", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    @Transactional
    public void resolveStaleDetectedIncidents() {
        try {
            int resolved = incidentRepository.resolveStaleDetected(
                IncidentStatus.DETECTED,
                OffsetDateTime.now().minusDays(4)
            );
            log.info("[PURGE] Resolved {} stale DETECTED incidents older than 4 days.", resolved);
        } catch (Exception e) {
            log.error("[PURGE] Failed to resolve stale DETECTED incidents", e);
        }
    }

    // CONFIRMED and STABLE staleness is now handled by the lifecycle FSM
}