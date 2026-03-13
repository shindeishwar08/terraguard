package com.terraguard.backend.api.controller;

import com.terraguard.backend.api.service.ImpactService;
import com.terraguard.backend.domain.dto.IncidentImpactResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class ImpactController {

    private final ImpactService impactService;

    @GetMapping("/{id}/impact")
    public ResponseEntity<IncidentImpactResponse> getIncidentImpact(@PathVariable UUID id) {
        IncidentImpactResponse impact = impactService.getImpactAnalysis(id);
        return ResponseEntity.ok(impact);
    }
}