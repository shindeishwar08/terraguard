package com.terraguard.backend.api.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.terraguard.backend.api.IncidentMapper;
import com.terraguard.backend.cache.CacheService;
import com.terraguard.backend.domain.dto.IncidentDetailResponse;
import com.terraguard.backend.domain.dto.IncidentTimelineDto;
import com.terraguard.backend.domain.dto.OverrideRequest;
import com.terraguard.backend.domain.entity.Incident;
import com.terraguard.backend.domain.entity.IncidentRepository;
import com.terraguard.backend.domain.entity.IncidentTimeline;
import com.terraguard.backend.domain.entity.IncidentTimelineRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentTimelineRepository incidentTimelineRepository;
    private final CacheService cacheService;
    private final IncidentMapper incidentMapper;

    public IncidentDetailResponse getIncidentDetail(UUID id) {
        Incident incident = incidentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found: " + id));
        return incidentMapper.toIncidentDetailResponse(incident);
    }

    public List<IncidentTimelineDto> getIncidentTimeline(UUID id) {
        return incidentTimelineRepository.findByIncidentIdOrderByCreatedAtDesc(id)
                .stream()
                .map(incidentMapper::toIncidentTimelineDto)
                .toList();
    }

    public Map<Object, Object> getSignalTallies(UUID id) {
        // HGETALL logic from Day 12 plan
        return cacheService.getSignalTally(id.toString());
    }

    @Transactional
    public void applyOverride(UUID id, OverrideRequest request) {
        
        Incident incident = incidentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        
        String oldStatus = incident.getStatus().name();

        incidentRepository.applyOverride(
            id, 
            request.getNewStatus().name(), 
            request.getReason(), 
            request.getOverrideBy()
        );


        IncidentTimeline timelineEntry = IncidentTimeline.builder()
            .incident(incident) 
            .previousStatus(oldStatus) 
            .newStatus(request.getNewStatus().name())
            .message("ADMIN OVERRIDE: " + request.getReason() + " (By: " + request.getOverrideBy() + ")")
            .createdAt(java.time.OffsetDateTime.now())
            .build();

        incidentTimelineRepository.save(timelineEntry);
    
        cacheService.setSnapshotDirty();
    }
}