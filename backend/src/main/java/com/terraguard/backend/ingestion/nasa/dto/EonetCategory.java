package com.terraguard.backend.ingestion.nasa.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EonetCategory {
    @JsonProperty("id")
    private String id;   // "wildfires", "severeStorms", "floods"

    @JsonProperty("title")
    private String title;
}