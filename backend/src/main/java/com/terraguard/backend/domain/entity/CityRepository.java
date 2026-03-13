// In CityRepository.java (new file in domain/entity/)
package com.terraguard.backend.domain.entity;

import com.terraguard.backend.domain.dto.CityImpactProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CityRepository extends JpaRepository<City, UUID> {

    @Query(value = """
        SELECT 
            c.name AS name,
            c.country AS country,
            ST_Distance(c.geometry::geography, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography) / 1000 AS distanceKm
        FROM cities c
        WHERE ST_DWithin(
            c.geometry::geography,
            ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
            :radiusMeters
        )
        ORDER BY distanceKm ASC
        """, nativeQuery = true)
    List<CityImpactProjection> findCitiesWithinRadius(
        @Param("lat") double lat,
        @Param("lon") double lon,
        @Param("radiusMeters") double radiusMeters
    );
}