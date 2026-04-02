package com.terraguard.backend.snapshot;

import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraguard.backend.api.IncidentMapper;
import com.terraguard.backend.cache.CacheService;
import com.terraguard.backend.domain.dto.GlobalEventResponse;
import com.terraguard.backend.domain.entity.Incident;
import com.terraguard.backend.domain.entity.IncidentRepository;
import com.terraguard.backend.domain.enums.IncidentStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnapshotCompilerService {
    
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;
    private final IncidentRepository incidentRepository;
    private final IncidentMapper incidentMapper;

    @Scheduled(fixedRate = 5000)
    public void compileSnapshot(){
        try {
            if(!cacheService.isSnapshotDirty()) return;

            log.debug("[SNAPSHOT] Dirty flag detected. Recompiling global state...");

            List<Incident> activeIncidents = incidentRepository.findByStatusNotIn(
                Arrays.asList(IncidentStatus.RESOLVED, IncidentStatus.ARCHIVED)
            );

            List<GlobalEventResponse> snapshot = new ArrayList<>();

            for(Incident active : activeIncidents){
                snapshot.add(incidentMapper.toGlobalEventResponse(active));
            }

            String jsonSnapshot = objectMapper.writeValueAsString(snapshot);

            cacheService.saveGlobalSnapshot(jsonSnapshot);
            cacheService.clearSnapshotDirty();

            log.info("[SNAPSHOT] Successfully compiled and cached {} active incidents.", snapshot.size());

        } catch (Exception e) {
            log.error("[SNAPSHOT] Failed to compile global snapshot", e);
        }
    }

    public String compileSnapshotFromDb() {
        try {
            List<Incident> activeIncidents = incidentRepository.findByStatusNotIn(
                Arrays.asList(IncidentStatus.RESOLVED, IncidentStatus.ARCHIVED)
            );

            List<GlobalEventResponse> snapshot = activeIncidents.stream()
                .map(incidentMapper::toGlobalEventResponse)
                .toList();

            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.error("[SNAPSHOT] Failed to compile snapshot from DB", e);
            return null;
        }
    }

}