package com.terraguard.backend.api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.terraguard.backend.api.IncidentMapper;
import com.terraguard.backend.cache.CacheService;
import com.terraguard.backend.domain.dto.GlobalEventResponse;
import com.terraguard.backend.domain.entity.IncidentRepository;
import com.terraguard.backend.snapshot.SnapshotCompilerService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {
    
    private final IncidentRepository incidentRepository;
    private final IncidentMapper incidentMapper;
    private final CacheService cacheService;
    private final SnapshotCompilerService snapshotCompilerService;

    public List<GlobalEventResponse> getNearbyEvents(double lat, double lon, double radiusKm) {
        double radiusMeters = radiusKm * 1000;
        
        return incidentRepository.findIncidentsWithinRadius(lat, lon, radiusMeters)
                .stream()
                .map(incidentMapper::toGlobalEventResponse)
                .toList();
    }


    public String getSnapshot() {
        String cached = cacheService.getGlobalSnapshot();
        if (cached != null) return cached;

        // Redis is down or cache miss — compile directly from DB
        return snapshotCompilerService.compileSnapshotFromDb();
    }

}
