// Mirrors: GlobalEventResponse.java
export interface GlobalEventResponse {
    id: string;
    title: string;
    disaster_type: string;        // DisasterType enum serialized as string
    status: string;               // IncidentStatus enum serialized as string
    severity_index: number;       // BigDecimal → number
    confidence_score: number;     // BigDecimal → number
    latitude: number;
    longitude: number;
    created_at: string;           // OffsetDateTime → ISO string
    updated_at: string;
}

// Mirrors: IncidentDetailResponse.java
export interface IncidentDetailResponse {
    id: string;
    title: string;
    source: string;               // DataSource enum serialized as string
    disaster_type: string;
    status: string;
    magnitude: number | null;     // nullable BigDecimal
    severity_index: number;
    confidence_score: number;
    latitude: number;
    longitude: number;
    contributing_sources: string;
    created_at: string;
    updated_at: string;
}

// Mirrors: IncidentTimelineDto.java
export interface IncidentTimelineDto {
    id: string;
    previous_status: string | null;   // nullable
    new_status: string;               // @JsonProperty("new_status") maps to status field
    message: string;
    created_at: string;
}

// Mirrors: CityImpactProjection.java interface
export interface CityImpact {
    name: string;
    country: string;
    distanceKm: number;           // getter getName() → camelCase in JSON
}

// Mirrors: IncidentImpactResponse.java
export interface IncidentImpactResponse {
    innerRing: CityImpact[];
    outerRing: CityImpact[];
}

// Mirrors: SignalType enum keys from Redis hash
export interface SignalTally {
    ROAD_BLOCKED?: number;
    POWER_OUTAGE?: number;
    MEDICAL_NEED?: number;
    MISINFORMATION?: number;
    ALL_CLEAR?: number;
}