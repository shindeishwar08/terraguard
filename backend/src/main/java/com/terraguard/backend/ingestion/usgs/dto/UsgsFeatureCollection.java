package com.terraguard.backend.ingestion.usgs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class UsgsFeatureCollection {

    @JsonProperty("features")
    private List<UsgsFeature> features;
}