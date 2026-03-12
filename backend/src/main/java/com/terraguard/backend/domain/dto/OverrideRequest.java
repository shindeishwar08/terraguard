package com.terraguard.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.terraguard.backend.domain.enums.IncidentStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OverrideRequest {

    @JsonProperty("new_status")
    private IncidentStatus newStatus;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("adminName")
    private String overrideBy;
}
