package com.terraguard.backend.ingestion.nasa.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EonetGeometry {

    @JsonProperty("type")
    private String type;  // "Point" or "Polygon"

    @JsonProperty("coordinates")
    private Object coordinates;  // Point: [lon,lat] — Polygon: [[[lon,lat],...]]

    @JsonProperty("magnitudeValue")
    private Double magnitudeValue;

    @JsonProperty("magnitudeUnit")  
    private String magnitudeUnit;

    @JsonProperty("date")
    private String date;
}