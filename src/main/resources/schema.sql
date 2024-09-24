CREATE TABLE IF NOT EXISTS geo_points (
    id SERIAL PRIMARY KEY,
    x DOUBLE PRECISION NOT NULL,
    y DOUBLE PRECISION NOT NULL,
    z DOUBLE PRECISION NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS geo_point_distances (
    geo_point_id BIGINT NOT NULL,
    anchor_id INT NOT NULL,  -- Anchor ID for each anchor point
    anchor_distance DOUBLE PRECISION NOT NULL,  -- The distance to the anchor
    FOREIGN KEY (geo_point_id) REFERENCES geo_points(id)
);

-- Index on timestamp for range queries
CREATE INDEX IF NOT EXISTS idx_timestamp ON geo_points (timestamp);

-- Indexes on geo_point_id and anchor_distance for better query performance
CREATE INDEX IF NOT EXISTS idx_anchor_distance ON geo_point_distances (anchor_distance);
CREATE INDEX IF NOT EXISTS idx_geo_point_anchor_distance ON geo_point_distances (geo_point_id, anchor_id, anchor_distance);
