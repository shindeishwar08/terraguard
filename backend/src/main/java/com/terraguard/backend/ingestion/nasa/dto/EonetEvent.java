package com.terraguard.backend.ingestion.nasa.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EonetEvent {

    @JsonProperty("id")
    private String id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("categories")
    private List<EonetCategory> categories;

    @JsonProperty("geometry")
    private List<EonetGeometry> geometry;  // array — multiple positions over time
}