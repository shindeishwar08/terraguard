package com.terraguard.backend.api;

import com.terraguard.backend.domain.dto.GlobalEventResponse;
import com.terraguard.backend.domain.dto.IncidentDetailResponse;
import com.terraguard.backend.domain.dto.IncidentTimelineDto;
import com.terraguard.backend.domain.entity.Incident;
import com.terraguard.backend.domain.entity.IncidentTimeline;
import org.springframework.stereotype.Component;

@Component
public class IncidentMapper {

    public GlobalEventResponse toGlobalEventResponse(Incident incident) {
        return GlobalEventResponse.builder()
                .id(incident.getId())
                .title(incident.getTitle())
                .disasterType(incident.getDisasterType())
                .status(incident.getStatus())
                .severityIndex(incident.getSeverityIndex())
                .confidenceScore(incident.getConfidenceScore())
                // PostGIS extraction: Y is Latitude, X is Longitude
                .latitude(incident.getGeometry().getY())
                .longitude(incident.getGeometry().getX())
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }

    public IncidentDetailResponse toIncidentDetailResponse(Incident incident) {
        return IncidentDetailResponse.builder()
                .id(incident.getId())
                .title(incident.getTitle())
                .source(incident.getSource())
                .disasterType(incident.getDisasterType())
                .status(incident.getStatus())
                .magnitude(incident.getMagnitude())
                .severityIndex(incident.getSeverityIndex())
                .confidenceScore(incident.getConfidenceScore())
                .latitude(incident.getGeometry().getY())
                .longitude(incident.getGeometry().getX())
                .contributingSources(incident.getContributingSources())
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }

    public IncidentTimelineDto toIncidentTimelineDto(IncidentTimeline timeline) {
        return IncidentTimelineDto.builder()
                .id(timeline.getId())
                .previousStatus(timeline.getPreviousStatus())
                .status(timeline.getNewStatus()) 
                .message(timeline.getMessage())
                .createdAt(timeline.getCreatedAt())
                .build();
    }
}