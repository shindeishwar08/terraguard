package com.terraguard.backend.ingestion.usgs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UsgsProperties {

    @JsonProperty("mag")
    private Double mag;        // nullable — some events have no magnitude

    @JsonProperty("place")
    private String place;

    @JsonProperty("time")
    private Long time;         // Unix milliseconds

    @JsonProperty("title")
    private String title;
}