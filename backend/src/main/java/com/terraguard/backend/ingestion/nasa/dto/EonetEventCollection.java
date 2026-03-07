package com.terraguard.backend.ingestion.nasa.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EonetEventCollection {
    @JsonProperty("events")
    private List<EonetEvent> events;
}