CREATE TABLE IF NOT EXISTS geo_points (
    id SERIAL PRIMARY KEY,
    x DOUBLE PRECISION NOT NULL,
    y DOUBLE PRECISION NOT NULL,
    z DOUBLE PRECISION NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    
    -- Array of distances to 10 anchor points
    anchor_distances DOUBLE PRECISION[] NOT NULL
);

-- Index on timestamp for range queries
CREATE INDEX IF NOT EXISTS idx_timestamp ON geo_points (timestamp);

-- GIN index for fast searching on array of anchor distances
CREATE INDEX IF NOT EXISTS idx_anchor_distances ON geo_points USING GIN (anchor_distances);
