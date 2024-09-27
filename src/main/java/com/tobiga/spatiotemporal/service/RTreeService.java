package com.tobiga.spatiotemporal.service;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;


@Service
public class RTreeService {

    private final JdbcTemplate jdbcTemplate;
    private final CoordinateService coordinateService;

    public RTreeService(JdbcTemplate jdbcTemplate, CoordinateService coordinateService) {
        this.jdbcTemplate = jdbcTemplate;
        this.coordinateService = coordinateService;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initializeGeopoints() {
        try {
            // Create the R-tree nodes table
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS rtree_nodes (" +
                    "id INTEGER PRIMARY KEY, " +
                    "parent_id INTEGER, " +
                    "minX REAL, maxX REAL, " +
                    "minY REAL, maxY REAL, " +
                    "minZ REAL, maxZ REAL, " +
                    "isLeaf BOOLEAN, " +
                    "FOREIGN KEY(parent_id) REFERENCES rtree_nodes(id)" +
                    ");");

            // Create the data points table (associated with leaf nodes)
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS data_points (" +
                    "id INTEGER PRIMARY KEY, " +
                    "node_id INTEGER, " +
                    "x REAL, y REAL, z REAL, " +
                    "externalId TEXT, " +
                    "timestamp INTEGER, " +
                    "FOREIGN KEY(node_id) REFERENCES rtree_nodes(id)" +
                    ");");

            System.out.println("R-tree and DataPoint tables created successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void insertGeopointWithLatLon(double lat, double lon, String externalId, long timestamp) {
        double[] xyz = coordinateService.convertLatLonToXYZ(lat, lon, 0);
        double x = xyz[0], y = xyz[1], z = xyz[2];

        // Generate a unique ID (can be based on timestamp or some logic)
        int id = (int) (timestamp / 1000); // Example: Use timestamp as ID

        // Insert the new geopoint into the appropriate leaf node
        String insertPointQuery = "INSERT INTO data_points (node_id, x, y, z, externalId, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(insertPointQuery, findLeafNodeForPoint(x, y, z), x, y, z, externalId, timestamp);

        System.out.println("Geopoint inserted: ID=" + id + ", X=" + x + ", Y=" + y + ", Z=" + z + ", External ID=" + externalId);
    }

    private int findLeafNodeForPoint(double x, double y, double z) {
        // Implement logic to find the appropriate leaf node using R-tree traversal
        // For now, assume 1 as the root node
        return 1;
    }

/*
    public List<DataPoint> findNearestNeighbors(Point queryPoint, int limit) {
        RTreeNode root = loadRootFromDatabase();
        PriorityQueue<RTreeNode> nodeQueue = new PriorityQueue<>(new DistanceComparator(queryPoint));
        nodeQueue.add(root);

        List<DataPoint> result = new ArrayList<>();
        while (!nodeQueue.isEmpty() && result.size() < limit) {
            RTreeNode currentNode = nodeQueue.poll();
            if (currentNode.isLeaf()) {
                result.addAll(currentNode.getDataPoints()); // Add points from leaf
            } else {
                nodeQueue.addAll(loadChildrenFromDatabase(currentNode)); // Traverse children
            }
        }
        return result;
    }

    private RTreeNode loadRootFromDatabase() {
        // SQL query to load the root node from the database
        return jdbcTemplate.queryForObject("SELECT * FROM rtree_nodes WHERE parent_id IS NULL",
                (rs, rowNum) -> new RTreeNode(
                        new BoundingBox(rs.getDouble("minX"), rs.getDouble("maxX"),
                                rs.getDouble("minY"), rs.getDouble("maxY"),
                                rs.getDouble("minZ"), rs.getDouble("maxZ")),
                        rs.getBoolean("isLeaf")));
    }

    private List<RTreeNode> loadChildrenFromDatabase(RTreeNode parent) {
        // SQL query to load children nodes from the database
        return jdbcTemplate.query("SELECT * FROM rtree_nodes WHERE parent_id = ?",
                new Object[]{parent.getId()},
                (rs, rowNum) -> new RTreeNode(
                        new BoundingBox(rs.getDouble("minX"), rs.getDouble("maxX"),
                                rs.getDouble("minY"), rs.getDouble("maxY"),
                                rs.getDouble("minZ"), rs.getDouble("maxZ")),
                        rs.getBoolean("isLeaf")));
    }
*/
}
