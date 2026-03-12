package com.terraguard.backend.api.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.terraguard.backend.purge.DataPurgeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/admin")
@RequiredArgsConstructor
@Profile("dev")
public class DevAdminController {
    
    private final DataPurgeService dataPurgeService;

    @PostMapping("/purge")
    public ResponseEntity<String> triggerManualPurge(){
        dataPurgeService.purgeArchivedIncidents();
        return ResponseEntity.ok("Manual purge triggered successfully.");
    }
}
