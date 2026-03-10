package com.terraguard.backend.domain.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface IncidentTimelineRepository
        extends JpaRepository<IncidentTimeline, UUID> {
}