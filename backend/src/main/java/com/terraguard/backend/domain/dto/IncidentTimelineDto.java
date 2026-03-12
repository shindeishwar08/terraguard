package com.terraguard.backend.domain.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class IncidentTimelineDto {
    
    @JsonProperty("id")
    private UUID id;

    @JsonProperty("previous_status")
    private String previousStatus;

    @JsonProperty("new_status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
}
