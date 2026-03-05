package com.terraguard.backend.domain.entity;

import com.terraguard.backend.domain.enums.DataSource;
import com.terraguard.backend.domain.enums.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}