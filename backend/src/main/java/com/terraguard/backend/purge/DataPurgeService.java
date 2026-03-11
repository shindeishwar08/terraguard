package com.terraguard.backend.purge;

import java.time.OffsetDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
}