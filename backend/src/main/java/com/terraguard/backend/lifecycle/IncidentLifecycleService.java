package com.terraguard.backend.lifecycle;

import com.terraguard.backend.domain.entity.Incident;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IncidentLifecycleService {

    public void evaluateTransition(Incident incident) {
        // Day 7: full FSM evaluation logic goes here
        log.debug("[FSM] Transition evaluation stubbed for incident: {}",
                incident.getExternalId());
    }
}