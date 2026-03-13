// domain/dto/IncidentImpactResponse.java
package com.terraguard.backend.domain.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class IncidentImpactResponse {
    private List<CityImpactProjection> innerRing; // < 50km
    private List<CityImpactProjection> outerRing; // < 100km
}