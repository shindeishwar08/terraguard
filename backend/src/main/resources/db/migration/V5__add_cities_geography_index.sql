DROP INDEX idx_cities_geometry;

CREATE INDEX idx_cities_geometry_geography 
    ON cities USING GIST(geometry::geography);