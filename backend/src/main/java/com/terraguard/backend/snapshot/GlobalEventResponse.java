package com.terraguard.backend.snapshot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalEventResponse {
    private UUID id;
    private String title;
    private double latitude;
    private double longitude;
    private String disasterType;
    private String status;
    private double severityIndex;
    private double confidenceScore;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}