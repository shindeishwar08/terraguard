package com.terraguard.backend.domain.entity;

import com.terraguard.backend.domain.enums.DataSource;
import com.terraguard.backend.domain.enums.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    // Idempotency check — used by every ingestion service
    Optional<Incident> findByExternalIdAndSource(String externalId, DataSource source);

    // Snapshot compiler query — excludes ARCHIVED & RESOLVED events
    List<Incident> findByStatusNotIn(Collection<IncidentStatus> statuses);

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
            override_locked, contributing_sources,
            geometry, created_at, updated_at
        ) VALUES (
            gen_random_uuid(), :title, :externalId, :source, :disasterType, 'DETECTED',
            :magnitude, 0, 0, false,
            ',' || :source || ',',
            ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326),
            NOW(), NOW()
        )
        ON CONFLICT (external_id, source) DO UPDATE SET
            magnitude            = EXCLUDED.magnitude,
            contributing_sources = CASE
                WHEN incidents.contributing_sources NOT LIKE '%,' || :source || ',%'
                THEN incidents.contributing_sources || :source || ','
                ELSE incidents.contributing_sources
            END,
            updated_at = NOW()
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

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE incidents SET
            severity_index   = :severity,
            confidence_score = :confidence,
            updated_at       = NOW()
        WHERE id = :id
    """, nativeQuery = true)
    void updateScores(
        @Param("id")         UUID id,
        @Param("severity")   BigDecimal severity,
        @Param("confidence") BigDecimal confidence
    );



    @Query(value = """
        SELECT COUNT(*) > 0 FROM incidents
        WHERE id != :id
        AND source != :source
        AND ST_DWithin(
            geometry::geography,
            (SELECT geometry::geography FROM incidents WHERE id = :id),
            :radiusMeters
        )
    """, nativeQuery = true)
    boolean existsNearbyFromDifferentSource(
        @Param("id")           UUID id,
        @Param("source")       String source,
        @Param("radiusMeters") double radiusMeters
    );

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE incidents 
        SET status = :status, updated_at = NOW() 
        WHERE id = :id
    """, nativeQuery = true)
    void updateStatus(@Param("id") UUID id, 
        @Param("status") String status
    );

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE incidents SET
            status          = :status,
            override_locked = true,
            override_reason = :reason,
            override_by     = :overrideBy,
            override_at     = NOW(),
            updated_at      = NOW()
        WHERE id = :id
        """, nativeQuery = true)
    void applyOverride(
        @Param("id")         UUID id,
        @Param("status")     String status,
        @Param("reason")     String reason,
        @Param("overrideBy") String overrideBy
    );  



    //  NIGHTLY PURGE JOB
    @Modifying
    @Transactional
    @Query("DELETE FROM Incident i WHERE i.status = :status AND i.updatedAt < :cutoffDate")
    int deleteByStatusAndUpdatedAtBefore(
        @Param("status") IncidentStatus status,
        @Param("cutoffDate") OffsetDateTime cutoffDate
    );
}