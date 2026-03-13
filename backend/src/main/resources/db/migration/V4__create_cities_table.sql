CREATE TABLE cities (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    country     VARCHAR(100),
    population  INTEGER,
    geometry    geometry(Point, 4326) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cities_geometry 
    ON cities USING GIST(geometry);

CREATE INDEX idx_cities_name 
    ON cities(name);