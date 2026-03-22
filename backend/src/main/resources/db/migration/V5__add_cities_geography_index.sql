-- V5__add_cities_geography_index.sql
DROP INDEX IF EXISTS idx_cities_geometry;
CREATE INDEX IF NOT EXISTS idx_cities_geometry_geography ON cities USING GIST(CAST(geometry AS geography));