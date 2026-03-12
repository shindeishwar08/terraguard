package com.terraguard.backend.api.controller;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terraguard.backend.api.service.EventService;
import com.terraguard.backend.domain.dto.GlobalEventResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/events")
public class EventsController {
    
    private final EventService eventService;

    @GetMapping("/snapshot")
    public ResponseEntity<String> getSnapshot(){
        String rawJsonString = eventService.getSnapshot();

        if (rawJsonString == null) return ResponseEntity.status(503).body("{\"error\":\"Snapshot unavailable\"}");

        return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(5, TimeUnit.SECONDS).cachePublic())
        .header("Content-Type", "application/json")
        .body(rawJsonString);
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<GlobalEventResponse>> getNearby(@RequestParam double lat, @RequestParam double lon, @RequestParam double radiusKm){
        List<GlobalEventResponse> nearbyEvents = eventService.getNearbyEvents(lat, lon, radiusKm);
        
        if (nearbyEvents == null || nearbyEvents.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(nearbyEvents);
    }

        
}
