package com.terraguard.backend.api.service;

import com.terraguard.backend.domain.dto.CityImpactProjection;
import com.terraguard.backend.domain.dto.IncidentImpactResponse;
import com.terraguard.backend.domain.entity.CityRepository;
import com.terraguard.backend.domain.entity.Incident;
import com.terraguard.backend.domain.entity.IncidentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImpactService {

    private final IncidentRepository incidentRepository;
    private final CityRepository cityRepository;

    @Transactional(readOnly = true)
    public IncidentImpactResponse getImpactAnalysis(UUID incidentId) {
        // Find the incident. We need its coordinates (geometry).
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Incident not found with ID: " + incidentId));

        // Fetch ALL cities within 100km (100,000 meters) in ONE DB call.
        List<CityImpactProjection> allImpacted = cityRepository.findCitiesWithinRadius(
                incident.getGeometry().getY(), // Latitude
                incident.getGeometry().getX(), // Longitude
                100000.0 // 100km radius
        );

        // Categorize them into rings using Java Streams.
        List<CityImpactProjection> innerRing = allImpacted.stream()
                .filter(city -> city.getDistanceKm() < 50)
                .toList();

        List<CityImpactProjection> outerRing = allImpacted.stream()
                .filter(city -> city.getDistanceKm() >= 50)
                .toList();

        return IncidentImpactResponse.builder()
                .innerRing(innerRing)
                .outerRing(outerRing)
                .build();
    }
}