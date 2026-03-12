package com.terraguard.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.terraguard.backend.domain.enums.DataSource;
import com.terraguard.backend.domain.enums.DisasterType;
import com.terraguard.backend.domain.enums.IncidentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class IncidentDetailResponse {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("source")
    private DataSource source;

    @JsonProperty("disaster_type")
    private DisasterType disasterType;

    @JsonProperty("status")
    private IncidentStatus status;

    @JsonProperty("magnitude")
    private BigDecimal magnitude;

    @JsonProperty("severity_index")
    private BigDecimal severityIndex;

    @JsonProperty("confidence_score")
    private BigDecimal confidenceScore;

    @JsonProperty("latitude")
    private double latitude;

    @JsonProperty("longitude")
    private double longitude;

    @JsonProperty("contributing_sources")
    private String contributingSources;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
}