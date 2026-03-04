CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE incidents (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title               VARCHAR(500) NOT NULL,
    external_id         VARCHAR(255) NOT NULL,
    source              VARCHAR(50)  NOT NULL,
    disaster_type       VARCHAR(50)  NOT NULL,
    status              VARCHAR(50)  NOT NULL DEFAULT 'DETECTED',

    magnitude           NUMERIC(5,2),
    severity_index      NUMERIC(5,2) NOT NULL DEFAULT 0
                            CHECK (severity_index >= 0 AND severity_index <= 100),
    confidence_score    NUMERIC(5,2) NOT NULL DEFAULT 0
                            CHECK (confidence_score >= 0 AND confidence_score <= 100),

    override_locked     BOOLEAN NOT NULL DEFAULT FALSE,
    override_reason     VARCHAR(500),
    override_by         VARCHAR(100),
    override_at         TIMESTAMP WITH TIME ZONE,

    geometry            geometry(Point, 4326) NOT NULL,

    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_incidents_external_source UNIQUE (external_id, source)
);

CREATE INDEX idx_incidents_geometry
    ON incidents USING GIST(geometry);

CREATE INDEX idx_incidents_status
    ON incidents(status);

CREATE INDEX idx_incidents_updated_at
    ON incidents(updated_at);