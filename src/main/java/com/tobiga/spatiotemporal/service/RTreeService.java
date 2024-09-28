package com.tobiga.spatiotemporal.service;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;


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
            // Create the R-tree nodes table if it doesn't exist
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS rtree_nodes (" +
                    "id INTEGER PRIMARY KEY, " +
                    "parent_id INTEGER, " +
                    "minX REAL, maxX REAL, " +
                    "minY REAL, maxY REAL, " +
                    "minZ REAL, maxZ REAL, " +
                    "isLeaf BOOLEAN, " +
                    "FOREIGN KEY(parent_id) REFERENCES rtree_nodes(id)" +
                    ");");

            // Create the data points table if it doesn't exist
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS data_points (" +
                    "id INTEGER PRIMARY KEY, " +
                    "node_id INTEGER, " +
                    "x REAL, y REAL, z REAL, " +
                    "externalId TEXT, " +
                    "timestamp INTEGER, " +
                    "FOREIGN KEY(node_id) REFERENCES rtree_nodes(id)" +
                    ");");

            // Create the metadata table to track the current root node
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS rtree_metadata (" +
                    "id INTEGER PRIMARY KEY, " +
                    "current_root_id INTEGER" +
                    ");");

            // Check if metadata exists
            Integer metadataExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM rtree_metadata WHERE id = 1", Integer.class);

            if (metadataExists == 0) {
                // Check if the root node already exists
                Integer rootExists = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM rtree_nodes WHERE id = 1", Integer.class);

                if (rootExists == 0) {
                    // Insert the initial root node if it doesn't exist
                    jdbcTemplate.update("INSERT INTO rtree_nodes (id, parent_id, minX, maxX, minY, maxY, minZ, maxZ, isLeaf) " +
                                    "VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?)",
                            1,  // Root node ID
                            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,  // Bounding box (entire space)
                            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                            true  // Root is a leaf initially
                    );
                    System.out.println("Root node inserted.");
                }

                // Insert the root ID into the metadata table
                jdbcTemplate.update("INSERT INTO rtree_metadata (id, current_root_id) VALUES (?, ?)",
                        1,  // Metadata entry ID
                        1   // Root node ID
                );
                System.out.println("Metadata initialized with root node ID.");
            } else {
                System.out.println("Metadata and root node already initialized.");
            }

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
        // Start by loading the root node
        RTreeNode currentNode = loadRootFromDatabase();

        // The point to insert
        Point point = new Point(x, y, z);

        // Traverse the tree until we reach a leaf node
        while (!currentNode.isLeaf()) {
            // Load the children of the current node
            List<RTreeNode> children = loadChildrenFromDatabase(currentNode);

            if (children.isEmpty()) {
                // Handle the case where there are no children
                System.out.println("No children found for node " + currentNode.getId() + ", treating as a leaf node.");
                return currentNode.getId();  // Return the current node if no children are found
            }

            // Find the child whose bounding box is closest to the point
            RTreeNode bestChild = null;
            double bestDistance = Double.MAX_VALUE;

            for (RTreeNode child : children) {
                double distance = child.getBoundingBox().distanceTo(point);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestChild = child;
                }
            }

            // Ensure bestChild is not null (shouldn't be null if children exist)
            if (bestChild == null) {
                throw new IllegalStateException("No suitable child node found for point " + point);
            }

            // Move down the tree to the selected child
            currentNode = bestChild;
        }

        // Return the ID of the leaf node
        return currentNode.getId();
    }

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

    private RTreeNode loadNodeFromDatabase(int nodeId) {
        String query = "SELECT * FROM rtree_nodes WHERE id = ?";

        return jdbcTemplate.queryForObject(query,
                new Object[]{nodeId},
                (rs, rowNum) -> new RTreeNode(
                        rs.getInt("id"),
                        new BoundingBox(rs.getDouble("minX"), rs.getDouble("maxX"),
                                rs.getDouble("minY"), rs.getDouble("maxY"),
                                rs.getDouble("minZ"), rs.getDouble("maxZ")),
                        rs.getBoolean("isLeaf")));
    }


    public RTreeNode loadRootFromDatabase() {
        Integer rootId = jdbcTemplate.queryForObject("SELECT current_root_id FROM rtree_metadata WHERE id = 1", Integer.class);
        return loadNodeFromDatabase(rootId);
    }

    private List<RTreeNode> loadChildrenFromDatabase(RTreeNode parent) {
        return jdbcTemplate.query("SELECT * FROM rtree_nodes WHERE parent_id = ?",
                new Object[]{parent.getId()},
                (rs, rowNum) -> new RTreeNode(
                        rs.getInt("id"),
                        new BoundingBox(rs.getDouble("minX"), rs.getDouble("maxX"),
                                rs.getDouble("minY"), rs.getDouble("maxY"),
                                rs.getDouble("minZ"), rs.getDouble("maxZ")),
                        rs.getBoolean("isLeaf")));
    }

    public void updateRootNode(int newRootId) {
        jdbcTemplate.update("UPDATE rtree_metadata SET current_root_id = ? WHERE id = 1", newRootId);
        System.out.println("Root node updated to " + newRootId);
    }
}
