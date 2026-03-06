package com.terraguard.backend.ingestion.usgs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UsgsFeature {

    @JsonProperty("id")
    private String id;  // ← this is our external_id

    @JsonProperty("properties")
    private UsgsProperties properties;

    @JsonProperty("geometry")
    private UsgsGeometry geometry;
}