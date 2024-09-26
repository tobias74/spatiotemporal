package com.tobiga.spatiotemporal.service;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SQLiteService {

    private final JdbcTemplate jdbcTemplate;
    private final CoordinateService coordinateService;

    public SQLiteService(JdbcTemplate jdbcTemplate, CoordinateService coordinateService) {
        this.jdbcTemplate = jdbcTemplate;
        this.coordinateService = coordinateService;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initializeGeopoints() {
        try {
            // Create the R-tree for spatial data
            String createRtreeSql = "CREATE VIRTUAL TABLE IF NOT EXISTS geopoints USING rtree("
                    + "id INTEGER PRIMARY KEY, "
                    + "minX REAL, maxX REAL, "
                    + "minY REAL, maxY REAL, "
                    + "minZ REAL, maxZ REAL)";
            jdbcTemplate.execute(createRtreeSql);
            System.out.println("R-tree table created successfully.");

            // Create the metadata table for non-spatial data (timestamp)
            String createMetadataTableSql = "CREATE TABLE IF NOT EXISTS geopoint_metadata ("
                    + "id INTEGER PRIMARY KEY, "
                    + "timestamp INTEGER, "
                    + "external_id TEXT)";
            jdbcTemplate.execute(createMetadataTableSql);
            System.out.println("Metadata table created successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // New method to insert geopoints with lat/lon/alt
    public void insertGeopointWithLatLon(double lat, double lon,  String externalId) {
        // Convert lat/lon/alt to XYZ coordinates
        double[] xyz = coordinateService.convertLatLonToXYZ(lat, lon, 0);
        long timestamp = System.currentTimeMillis();

        // Generate a unique ID (use timestamp or another strategy)
        int id = (int) (timestamp / 1000); // Example: Use timestamp (in seconds) as ID

        // Insert into R-tree table (geopoints)
        String insertRtreeSql = "INSERT INTO geopoints (id, minX, maxX, minY, maxY, minZ, maxZ) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(insertRtreeSql, id, xyz[0], xyz[0], xyz[1], xyz[1], xyz[2], xyz[2]);

        // Insert into metadata table (geopoint_metadata with external_id)
        String insertMetadataSql = "INSERT INTO geopoint_metadata (id, timestamp, external_id) "
                + "VALUES (?, ?, ?)";
        jdbcTemplate.update(insertMetadataSql, id, timestamp, externalId);

        System.out.println("Geopoint inserted: ID=" + id + ", Lat=" + lat + ", Lon=" + lon + ", External ID=" + externalId);
    }

}
