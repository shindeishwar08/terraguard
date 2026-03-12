package com.terraguard.backend.api.controller;

import com.terraguard.backend.api.service.IncidentService;
import com.terraguard.backend.domain.dto.IncidentDetailResponse;
import com.terraguard.backend.domain.dto.IncidentTimelineDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    // 1. Full Incident Detail
    @GetMapping("/{id}")
    public ResponseEntity<IncidentDetailResponse> getIncidentDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(incidentService.getIncidentDetail(id));
    }

    // 2. Incident Timeline (Audit Log)
    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<IncidentTimelineDto>> getIncidentTimeline(@PathVariable UUID id) {
        return ResponseEntity.ok(incidentService.getIncidentTimeline(id));
    }

    // 3. Crowd Signal Tallies from Redis
    @GetMapping("/{id}/signals")
    public ResponseEntity<Map<Object, Object>> getSignalTallies(@PathVariable UUID id) {
        return ResponseEntity.ok(incidentService.getSignalTallies(id));
    }
}