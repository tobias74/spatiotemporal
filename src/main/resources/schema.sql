CREATE TABLE IF NOT EXISTS geo_points (
    id SERIAL PRIMARY KEY,
    x DOUBLE PRECISION NOT NULL,
    y DOUBLE PRECISION NOT NULL,
    z DOUBLE PRECISION NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS geo_point_distances (
    geo_point_id BIGINT NOT NULL,
    anchor_distance DOUBLE PRECISION NOT NULL,
    FOREIGN KEY (geo_point_id) REFERENCES geo_points(id)
);

-- Index on timestamp for range queries
CREATE INDEX IF NOT EXISTS idx_timestamp ON geo_points (timestamp);
