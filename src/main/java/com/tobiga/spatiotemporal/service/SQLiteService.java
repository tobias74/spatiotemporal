package com.tobiga.spatiotemporal.service;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SQLiteService {

    private final JdbcTemplate jdbcTemplate;

    public SQLiteService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initialize() {
        try {
            // Create a test table
            String createTableSql = "CREATE TABLE IF NOT EXISTS test_table (id INTEGER PRIMARY KEY, name TEXT)";
            jdbcTemplate.execute(createTableSql);
            System.out.println("Table created successfully.");

            // Insert a row
            String insertSql = "INSERT INTO test_table (name) VALUES ('Sample Name')";
            jdbcTemplate.update(insertSql);
            System.out.println("Data inserted successfully.");

            // Query the data
            String querySql = "SELECT * FROM test_table";
            jdbcTemplate.query(querySql, (rs, rowNum) -> {
                System.out.println("ID: " + rs.getInt("id") + ", Name: " + rs.getString("name"));
                return null;
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // New initialize method for the R-tree and metadata approach
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
                    + "timestamp INTEGER)";
            jdbcTemplate.execute(createMetadataTableSql);
            System.out.println("Metadata table created successfully.");

            // Insert sample data into both tables
            insertSampleGeopoint(1, 0.0, 0.0, 0.0, System.currentTimeMillis());
            insertSampleGeopoint(2, 10.0, 10.0, 10.0, System.currentTimeMillis());

            // Query the R-tree and metadata table
            String querySql = "SELECT g.id, g.minX, g.minY, g.minZ, m.timestamp "
                    + "FROM geopoints g "
                    + "JOIN geopoint_metadata m ON g.id = m.id "
                    + "WHERE m.timestamp > ? "
                    + "ORDER BY m.timestamp ASC";

            jdbcTemplate.query(querySql, new Object[]{System.currentTimeMillis() - 1000}, (rs, rowNum) -> {
                System.out.println("ID: " + rs.getInt("id") + ", minX: " + rs.getDouble("minX")
                        + ", minY: " + rs.getDouble("minY") + ", minZ: " + rs.getDouble("minZ")
                        + ", Timestamp: " + rs.getLong("timestamp"));
                return null;
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper method to insert data into both the R-tree and metadata tables
    private void insertSampleGeopoint(int id, double x, double y, double z, long timestamp) {
        try {
            // Insert into R-tree (geopoints)
            String insertRtreeSql = "INSERT INTO geopoints (id, minX, maxX, minY, maxY, minZ, maxZ) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(insertRtreeSql, id, x, x, y, y, z, z);

            // Insert into metadata table (geopoint_metadata)
            String insertMetadataSql = "INSERT INTO geopoint_metadata (id, timestamp) "
                    + "VALUES (?, ?)";
            jdbcTemplate.update(insertMetadataSql, id, timestamp);

            System.out.println("Geopoint " + id + " inserted successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
