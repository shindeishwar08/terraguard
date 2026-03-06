package com.terraguard.backend.domain.entity;

import com.terraguard.backend.domain.enums.DataSource;
import com.terraguard.backend.domain.enums.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    // Idempotency check — used by every ingestion service
    Optional<Incident> findByExternalIdAndSource(String externalId, DataSource source);

    // Snapshot compiler query — excludes ARCHIVED events
    List<Incident> findByStatusNot(IncidentStatus status);

    // PostGIS spatial query — core of the nearby feature
    @Query(value = """
            SELECT * FROM incidents
            WHERE ST_DWithin(
                geometry::geography,
                ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                :radiusMeters
            )
            """, nativeQuery = true)
    List<Incident> findIncidentsWithinRadius(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusMeters") double radiusMeters
    );


    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO incidents (
            id, title, external_id, source, disaster_type, status,
            magnitude, severity_index, confidence_score,
            override_locked, geometry, created_at, updated_at
        ) VALUES (
            gen_random_uuid(), :title, :externalId, :source, :disasterType, 'DETECTED',
            :magnitude, 0, 0, false,
            ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326),
            NOW(), NOW()
        )
        ON CONFLICT (external_id, source) DO UPDATE SET
            magnitude     = EXCLUDED.magnitude,
            updated_at    = NOW()
    """, nativeQuery = true)
    void upsertIncident(
        @Param("title")        String title,
        @Param("externalId")   String externalId,
        @Param("source")       String source,
        @Param("disasterType") String disasterType,
        @Param("magnitude")    Double magnitude,
        @Param("longitude")    double longitude,
        @Param("latitude")     double latitude
);
}