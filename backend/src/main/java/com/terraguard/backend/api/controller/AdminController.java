package com.terraguard.backend.api.controller;

import com.terraguard.backend.api.service.IncidentService;
import com.terraguard.backend.domain.dto.OverrideRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final IncidentService incidentService;

    @PostMapping("/incidents/{id}/override")
    public ResponseEntity<Map<String, String>> applyOverride(
            @PathVariable UUID id, 
            @RequestBody OverrideRequest request) {
        
        incidentService.applyOverride(id, request);
        
        return ResponseEntity.ok(Map.of(
            "message", "Incident " + id + " manually locked to " + request.getNewStatus(),
            "status", "SUCCESS"
        ));
    }
}