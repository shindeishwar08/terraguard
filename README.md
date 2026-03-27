# TerraGuard — Real-Time Humanitarian Intelligence Platform

> Live at **[terraguard.app](https://terraguard.app)** — tracking 90+ active global disasters in real time.

TerraGuard is an open-source humanitarian intelligence platform that ingests live disaster data from USGS, NASA EONET, and GDACS, processes it through a weighted scoring engine and Finite State Machine lifecycle, and renders it on an interactive world map with real-time crowd-sourced signal aggregation and per-incident chatrooms.

---

## Architecture
```
Users (Browser)
       ↓ HTTPS terraguard.app
Hostinger CDN  (React + MapLibre + Deck.gl)
       ↓ HTTPS api.terraguard.app
Nginx + SSL  (EC2 ARM — Let's Encrypt)
       ↓ HTTP :8080
Spring Boot 4.0
  ├── REST APIs         → snapshot, nearby, incident detail, impact radius
  ├── WebSocket/STOMP   → real-time chat per incident
  └── Scheduler         → ingestion, FSM evaluation, snapshot compiler
       ├── PostgreSQL + PostGIS  (incidents, timeline, 47,868 cities)
       └── Redis                 (snapshot cache, signal tallies, chat history)
            ↑
  USGS (60s) · NASA EONET (300s) · GDACS (300s)
```

---

## Tech Stack

| Technology | Why chosen |
|---|---|
| **Java 21 + Spring Boot 4.0** | Virtual threads, structured concurrency, production-grade ecosystem |
| **PostgreSQL + PostGIS** | Spatial queries with GIST indexes — `ST_DWithin` for proximity detection |
| **Redis** | O(1) snapshot serving, atomic signal aggregation via Hash, LTRIM chat windows |
| **Flyway** | Versioned schema migrations — reproducible DB state across environments |
| **Docker + Docker Compose** | Consistent environment across Mac and EC2 ARM |
| **AWS EC2 t4g.small (ARM)** | 2GB RAM Graviton — 20% cheaper than x86 at same performance |
| **Nginx + Let's Encrypt** | HTTPS reverse proxy — mixed content prevention |
| **React + Vite** | Fast builds, `.env.production` baking of API URL |
| **MapLibre GL JS** | Open-source WebGL map rendering — no API key required |
| **Deck.gl** | GPU-accelerated ScatterplotLayer for 90+ concurrent incident markers |
| **WebSocket/STOMP** | Persistent bidirectional connection for real-time chatrooms |
| **Hostinger CDN** | Static asset distribution — global edge caching |

---

## Key Engineering Decisions

### Idempotent UPSERT with conditional `updated_at`
Every ingestion uses `INSERT ... ON CONFLICT (external_id, source) DO UPDATE`. The `updated_at` field only changes when `magnitude IS DISTINCT FROM EXCLUDED.magnitude OR contributing_sources` changes — not on every re-ingestion. This prevents the FSM stability timer from resetting unnecessarily and avoids phantom state transitions.

### Dirty-flag snapshot pattern
Instead of recompiling the global snapshot on every incident update, a Redis boolean flag (`snapshot:dirty`) is set. `SnapshotCompilerService` checks every 5 seconds — if dirty, it fetches all active incidents, serializes to JSON, caches in Redis, and clears the flag. No matter how many incidents update per cycle, exactly one recompile occurs. Served with `Cache-Control: public, max-age=5` for CDN-level caching.

### Finite State Machine with override locking
Six states: `DETECTED → CONFIRMED → ESCALATING ↔ STABLE → RESOLVED → ARCHIVED`. Transitions are driven by severity scores, source confirmation, crowd signals, and graduated time-based staleness decay. `override_locked=true` permanently blocks FSM engine for any incident — human judgment overrides automated transitions. Every transition appends an immutable audit entry to `incident_timeline`.

### O(1) signal aggregation via Redis Hash
Crowd signals (`ROAD_BLOCKED`, `MEDICAL_NEED` etc.) are stored as Redis Hash fields with `HINCRBY` — atomic increment, no locks. `HGETALL` is O(1) regardless of message volume. Compared to SQL `COUNT(*) GROUP BY signal_type`, Redis returns in microseconds at any scale. TTL set to 15 days matching the incident purge window.

### PostGIS geography-cast GIST index
`CREATE INDEX ON cities USING GIST(CAST(geometry AS geography))` — geography type accounts for Earth's curvature, critical for accurate long-distance proximity queries. `ST_DWithin` with this index drops from 731ms to 55ms across 47,868 cities (13x improvement). Note: `::` cast shorthand is unsupported in `CREATE INDEX` — explicit `CAST()` required.

### Stale FSM evaluator
A scheduled job runs every 5 minutes evaluating `ESCALATING`, `CONFIRMED`, `STABLE`, and `RESOLVED` incidents not updated in 2+ hours. Uses graduated staleness decay — CONFIRMED incidents stabilize after 6 hours of no source activity regardless of severity, STABLE incidents resolve after 6 hours (low severity) or 48 hours (any severity), and RESOLVED incidents archive after 3 days. Complements the nightly purge that force-resolves `DETECTED` incidents after 4 days and deletes `ARCHIVED` incidents after 15 days.

---

<!-- ## Memory Budget (2GB EC2)
```
OS + Docker:   400MB
JVM heap:      650MB   (-Xmx650m -Xms256m -XX:+UseG1GC)
Redis:         150MB   (maxmemory 130mb, allkeys-lru)
PostgreSQL:    300MB   (mem_limit: 300m)
Buffer:        548MB
─────────────────────
Total:        2048MB
``` -->

<!-- Measured heap at 5-6 concurrent users: 268MB. Redis at 90 active incidents: 22MB. -->

<!-- --- -->

## API Reference

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/events/snapshot` | All active incidents from Redis cache |
| `GET` | `/api/v1/events/nearby?lat&lon&radiusKm` | Incidents within radius (PostGIS) |
| `GET` | `/api/v1/incidents/{id}` | Full incident detail |
| `GET` | `/api/v1/incidents/{id}/timeline` | FSM transition audit log |
| `GET` | `/api/v1/incidents/{id}/signals` | Crowd signal tallies from Redis |
| `GET` | `/api/v1/incidents/{id}/impact` | Cities within 50km and 50-100km |
| `POST` | `/api/v1/admin/incidents/{id}/override` | Lock incident, set manual status |
| `WS` | `/ws` (STOMP) | Real-time chat connection |

Health: `GET /actuator/health`

---

## Scoring Engine

### Severity Index (0–100)
| Disaster | Formula |
|---|---|
| Earthquake | `magnitude × 10`, capped at 100 |
| Wildfire | `min(area_km² / 10, 85) + 15`, capped at 100 |
| Flood / Cyclone | Alert score: Green=25, Orange=55, Red=85 |
| Cyclone (GDACS) | Wind speed: ≥120 km/h=85, ≥63=55, else 25 |

### Confidence Score (0–100)
| Source | Weight |
|---|---|
| USGS | +40 |
| NASA EONET | +20 |
| GDACS | +20 |
| Crowd signals | +1 per 10 signals, capped at +10 |

Ranges: 0–33 = LOW, 34–66 = MEDIUM, 67–100 = HIGH.

---

## Running Locally

### Prerequisites
- Java 21
- Docker + Docker Compose
- Node.js 20+

### Backend
```bash
cd backend
docker-compose up -d          # starts PostgreSQL + Redis
./mvnw spring-boot:run
```

### Frontend
```bash
cd frontend
cp .env.example .env          # set VITE_API_BASE_URL=http://localhost:8080
npm install
npm run dev
```

---

## Project Structure
```
TerraGuard/
├── backend/
│   ├── src/main/java/com/terraguard/backend/
│   │   ├── ingestion/          # USGS, NASA, GDACS ingestion services
│   │   ├── scoring/            # ScoringEngineService
│   │   ├── lifecycle/          # FSM — IncidentLifecycleService
│   │   ├── snapshot/           # SnapshotCompilerService
│   │   ├── cache/              # CacheService, RedisKeys
│   │   ├── websocket/          # ChatController, ChatRateLimiter
│   │   ├── api/                # REST controllers, DTOs, mappers
│   │   ├── purge/              # DataPurgeService
│   │   └── domain/             # Entities, enums, repositories
│   └── src/main/resources/
│       └── db/migration/       # Flyway V1–V5 migrations
└── frontend/
    └── src/
        ├── components/         # MapView, IncidentHub, ChatTab, SignalsTab
        ├── hooks/              # useSnapshot, useSignals, useChatRoom
        ├── context/            # MapContext
        └── api/                # fetchSnapshot, fetchSignalTally etc.
```

---

## Data Sources

| Source | Disasters | Poll interval | Notes |
|---|---|---|---|
| USGS Earthquake Hazards | Earthquakes | 60s | Real magnitude from API |
| NASA EONET | Wildfires, Cyclones, Floods | 300s | Filters: area > 10km², excludes prescribed burns |
| GDACS | Earthquakes, Floods, Cyclones, Wildfires | 300s | Alert level: Green filtered for wildfires |

---

## Deployment

<!-- **Backend** — AWS EC2 t4g.small (ARM, Ubuntu 22.04), Docker Compose, systemd auto-restart. -->
**Backend** — AWS EC2 (ARM, Ubuntu 22.04), Docker Compose, systemd auto-restart.

**Frontend** — Hostinger CDN, static `dist/` upload, `.htaccess` SPA fallback.

**SSL** — Let's Encrypt via Certbot + Nginx reverse proxy. `api.terraguard.app` → EC2 `:8080`.

---

## Known Limitations

<!-- 1. GDACS magnitude stored as alert score for older records (pre-fix ingestions) -->
1. Crowd signals affect confidence on next ingestion cycle only
2. No cross-source deduplication for geographically proximate duplicate events
3. WebSocket no authentication — display names are self-reported
4. Impact radius only shows top 5 cities — UI truncates to 5 per ring even if 100 cities exist
5. Map doesn't highlight nearby events — useGeolocation fetches nearby events but they're not visually          distinguished from global events on the map

---

<!-- ## Roadmap

- [ ] Gemini-powered incident briefing (v1.1)
- [ ] Threshold-based confidence score refresh on crowd signal surge
- [ ] Redis Pub/Sub for multi-instance WebSocket broadcasting
- [ ] Prometheus + Grafana observability dashboard
- [ ] TSUNAMI, VOLCANO, LANDSLIDE disaster types
- [ ] Pacific Tsunami Warning Center, EFAS data sources
- [ ] Remove hardcoded credentials from docker-compose.yml before open source

--- -->

## Author

**Ishwar Shinde** — [GitHub](https://github.com/shindeishwar08) · [LinkedIn](https://linkedin.com/in/ishwar-shinde-253984288/)

---

## License

MIT License — open source, contributions welcome.