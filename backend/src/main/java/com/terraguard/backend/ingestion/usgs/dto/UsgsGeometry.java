package com.terraguard.backend.ingestion.usgs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class UsgsGeometry {

    @JsonProperty("coordinates")
    private List<Double> coordinates;  // [longitude, latitude, depth]

    public double getLongitude() { return coordinates.get(0); }
    public double getLatitude()  { return coordinates.get(1); }
    public double getDepth()     { return coordinates.get(2); }
}