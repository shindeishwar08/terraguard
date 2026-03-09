package com.terraguard.backend.domain.entity;

import com.terraguard.backend.domain.enums.DataSource;
import com.terraguard.backend.domain.enums.DisasterType;
import com.terraguard.backend.domain.enums.IncidentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 50)
    private DataSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "disaster_type", nullable = false, length = 50)
    private DisasterType disasterType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private IncidentStatus status;

    @Column(name = "magnitude", precision = 5, scale = 2)
    private BigDecimal magnitude;

    @Column(name = "severity_index", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal severityIndex = BigDecimal.ZERO;

    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal confidenceScore = BigDecimal.ZERO;

    @Column(name = "override_locked", nullable = false)
    @Builder.Default
    private Boolean overrideLocked = false;

    @Column(name = "override_reason", length = 500)
    private String overrideReason;

    @Column(name = "override_by", length = 100)
    private String overrideBy;

    @Column(name = "override_at")
    private OffsetDateTime overrideAt;

    @Column(name = "geometry", nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point geometry;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    //New Alteration
    @Column(name="contributing_sources", nullable = false)
    private String contributingSources;
}