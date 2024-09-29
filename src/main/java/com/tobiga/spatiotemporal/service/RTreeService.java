package com.tobiga.spatiotemporal.service;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.PriorityQueue;

import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;


@Service
public class RTreeService {

    private final JdbcTemplate jdbcTemplate;
    private final CoordinateService coordinateService;

    private static final int NODE_CAPACITY = 4;
    private static final int UNDERFLOW_THRESHOLD = 2;


    private SplitStrategy splitStrategy;

    public RTreeService(JdbcTemplate jdbcTemplate, CoordinateService coordinateService) {
        this.jdbcTemplate = jdbcTemplate;
        this.coordinateService = coordinateService;
        this.splitStrategy = new QuadraticSplitStrategy(this);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initializeGeopoints() {
        try {
            // Create the R-tree nodes table if it doesn't exist
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS rtree_nodes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "parent_id INTEGER, " +
                    "minX REAL, maxX REAL, " +
                    "minY REAL, maxY REAL, " +
                    "minZ REAL, maxZ REAL, " +
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
                    jdbcTemplate.update("INSERT INTO rtree_nodes (id, parent_id, minX, maxX, minY, maxY, minZ, maxZ ) " +
                                    "VALUES (?, NULL, ?, ?, ?, ?, ?, ?)",
                            1,  // Root node ID
                            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,  // Bounding box (entire space)
                            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY
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
        // Convert latitude/longitude to XYZ coordinates
        double[] xyz = coordinateService.convertLatLonToXYZ(lat, lon, 0);
        double x = xyz[0], y = xyz[1], z = xyz[2];

        // Create a DataPoint object using the converted coordinates and other information
        DataPoint dataPoint = new DataPoint(x, y, z, externalId, timestamp);

        // Insert the data point into the appropriate node in the R-tree
        insertDataPoint(dataPoint);

        System.out.println("Geopoint inserted: Lat=" + lat + ", Lon=" + lon + ", External ID=" + externalId);
    }

    public void insertDataPoint(DataPoint point) {
        // Find the best leaf node to insert the data point
        int leafNodeId = findLeafNodeForPoint(point.getX(), point.getY(), point.getZ());
        RTreeNode leafNode = loadNodeFromDatabase(leafNodeId);

        // Insert the data point into the leaf node
        jdbcTemplate.update("INSERT INTO data_points (node_id, x, y, z, externalId, timestamp) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                leafNode.getId(), point.getX(), point.getY(), point.getZ(),
                point.getExternalId(), point.getTimestamp());

        int dataPointCount = countDataPointsInNode(leafNodeId);

        if (dataPointCount > NODE_CAPACITY) {
            splitNodeRecursive(leafNode);
        }
    }

    public void updateDataPointNodeId(int newNodeId, DataPoint point) {
        System.out.println("update datapoint to new node id " + newNodeId + " and point id " + point.getId());
        jdbcTemplate.update("UPDATE data_points SET node_id = ? WHERE id = ?",
                newNodeId, point.getId());
    }

    private int findLeafNodeForPoint(double x, double y, double z) {
        // Start by loading the root node
        RTreeNode currentNode = loadRootFromDatabase();

        // The point to insert
        Point point = new Point(x, y, z);

        // Traverse the tree until we reach a leaf node
        while (true) {
            // Load the children of the current node
            List<RTreeNode> children = loadChildrenFromDatabase(currentNode);

            // Check if the current node has no children, meaning it's a leaf
            if (children.isEmpty()) {
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
    }

    private RTreeNode loadNodeFromDatabase(int nodeId) {
        // Retrieve the current root node ID from metadata
        Integer rootId = jdbcTemplate.queryForObject("SELECT current_root_id FROM rtree_metadata WHERE id = 1", Integer.class);

        System.out.println("This is our nodeId to be loaded: " + nodeId);

        // Query to load the node by its ID, including parent_id
        String query = "SELECT * FROM rtree_nodes WHERE id = ?";
        return jdbcTemplate.queryForObject(query, new Object[]{nodeId}, (rs, rowNum) -> {
            boolean isRoot = (nodeId == rootId);  // Compare the node ID with the root ID

            // Try to fetch parent_id and check if it is null
            Integer parentId = rs.getObject("parent_id") != null ? rs.getInt("parent_id") : null;

            // Return the RTreeNode with the isRoot property and parentId set correctly
            return new RTreeNode(
                    rs.getInt("id"),
                    parentId,  // parentId will be null for the root node
                    new BoundingBox(rs.getDouble("minX"), rs.getDouble("maxX"),
                            rs.getDouble("minY"), rs.getDouble("maxY"),
                            rs.getDouble("minZ"), rs.getDouble("maxZ")),
                    isRoot
            );
        });
    }


    public RTreeNode loadRootFromDatabase() {
        Integer rootId = jdbcTemplate.queryForObject("SELECT current_root_id FROM rtree_metadata WHERE id = 1", Integer.class);
        System.out.println("This is our rootId" + rootId);
        return loadNodeFromDatabase(rootId);
    }

    private List<RTreeNode> loadChildrenFromDatabase(RTreeNode parent) {
        // Retrieve the current root node ID from metadata
        Integer rootId = jdbcTemplate.queryForObject("SELECT current_root_id FROM rtree_metadata WHERE id = 1", Integer.class);

        // Query for the child nodes
        return jdbcTemplate.query("SELECT * FROM rtree_nodes WHERE parent_id = ?",
                new Object[]{parent.getId()},
                (rs, rowNum) -> {
                    int nodeId = rs.getInt("id");
                    boolean isRoot = (nodeId == rootId);  // Compare the node ID with the root ID

                    // Properly handle the potential null value for parent_id
                    Integer parentId = rs.getObject("parent_id") != null ? rs.getInt("parent_id") : null;

                    // Return the RTreeNode with the isRoot property and parentId set correctly
                    return new RTreeNode(
                            nodeId,
                            parentId,  // parentId will be null for the root node
                            new BoundingBox(rs.getDouble("minX"), rs.getDouble("maxX"),
                                    rs.getDouble("minY"), rs.getDouble("maxY"),
                                    rs.getDouble("minZ"), rs.getDouble("maxZ")),
                            isRoot
                    );
                });
    }


    public void updateRootNode(int newRootId) {
        jdbcTemplate.update("UPDATE rtree_metadata SET current_root_id = ? WHERE id = 1", newRootId);
        System.out.println("Root node updated to " + newRootId);
    }

    private int getNodeSize(int nodeId) {
        // SQL query to count the number of children for the given node
        String query = "SELECT COUNT(*) FROM rtree_nodes WHERE parent_id = ?";
        return jdbcTemplate.queryForObject(query, new Object[]{nodeId}, Integer.class);
    }

    public void insertNewNode(RTreeNode node) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        String sql = "INSERT INTO rtree_nodes (parent_id, minX, maxX, minY, maxY, minZ, maxZ) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setObject(1, node.getParentId());  // parent_id can be null
            ps.setDouble(2, node.getBoundingBox().getMinX());
            ps.setDouble(3, node.getBoundingBox().getMaxX());
            ps.setDouble(4, node.getBoundingBox().getMinY());
            ps.setDouble(5, node.getBoundingBox().getMaxY());
            ps.setDouble(6, node.getBoundingBox().getMinZ());
            ps.setDouble(7, node.getBoundingBox().getMaxZ());
            return ps;
        }, keyHolder);

        // Retrieve the auto-generated ID from the database and update the node
        node.setId(keyHolder.getKey().intValue());
    }

    public void updateParent(int parentId, int childId) {
        jdbcTemplate.update("UPDATE rtree_nodes SET parent_id = ? WHERE id = ?", parentId, childId);
    }

    public void deleteNode(int nodeId) {
        jdbcTemplate.update("DELETE FROM rtree_nodes WHERE id = ?", nodeId);
    }


    private void splitNodeRecursive(RTreeNode node) {
        splitStrategy.splitNode(node);

        // In case of a non-root node, handle recursive splitting of parent if needed
        if (!node.isRoot()) {
            RTreeNode parentNode = loadNodeFromDatabase(node.getParentId());
            if (getNodeSize(parentNode.getId()) > NODE_CAPACITY) {
                splitNodeRecursive(parentNode);  // Recursively split the parent
            }
        }
    }


    public void handleRootSplit(RTreeNode newNode1, RTreeNode newNode2, RTreeNode oldRoot) {
        // Create a new root node with bounding box spanning from -Infinity to +Infinity
        BoundingBox newRootBoundingBox = new BoundingBox(
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,  // X bounds
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,  // Y bounds
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY   // Z bounds
        );

        // Create the new root node
        RTreeNode newRoot = new RTreeNode(null, null, newRootBoundingBox, true);

        // Insert the new root node into the database and update its ID
        insertNewNode(newRoot);

        // Update the parent ID of the new child nodes to point to the new root
        updateParent(newRoot.getId(), newNode1.getId());
        updateParent(newRoot.getId(), newNode2.getId());

        // Update the metadata to reflect the new root node
        updateRootNode(newRoot.getId());

        // Optionally delete the old root node (if necessary)
        deleteNode(oldRoot.getId());
    }


    public List<DataPoint> loadDataPointsFromNode(int nodeId) {
        String query = "SELECT * FROM data_points WHERE node_id = ?";
        return jdbcTemplate.query(query, new Object[]{nodeId}, (rs, rowNum) ->
                new DataPoint(
                        rs.getInt("id"),   // Capture the ID from the result set
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getString("externalId"),
                        rs.getLong("timestamp")
                )
        );
    }

    private int countDataPointsInNode(int nodeId) {
        String query = "SELECT COUNT(*) FROM data_points WHERE node_id = ?";
        return jdbcTemplate.queryForObject(query, new Object[]{nodeId}, Integer.class);
    }


    public void deleteGeopointByExternalId(String externalId) {
        // Fetch all data points with the external ID
        List<DataPoint> pointsToDelete = jdbcTemplate.query(
                "SELECT * FROM data_points WHERE externalId = ?",
                new Object[]{externalId},
                (rs, rowNum) -> new DataPoint(
                        rs.getInt("id"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getString("externalId"),
                        rs.getLong("timestamp")
                )
        );

        if (pointsToDelete.isEmpty()) {
            throw new IllegalArgumentException("No DataPoints found for externalId: " + externalId);
        }

        // Loop through each point and handle them individually
        for (DataPoint pointToDelete : pointsToDelete) {
            // Delete the data point from the database
            jdbcTemplate.update("DELETE FROM data_points WHERE id = ?", pointToDelete.getId());

            // Now check if the node that contained this point underflows
            int nodeId = findLeafNodeForPoint(pointToDelete.getX(), pointToDelete.getY(), pointToDelete.getZ());

            // Handle underflow for this specific node
            handleUnderflow(nodeId);
        }
    }


    private void handleUnderflow(int nodeId) {
        RTreeNode node = loadNodeFromDatabase(nodeId);

        // Use the number of children to check if the node is a leaf node
        List<RTreeNode> childNodes = loadChildrenFromDatabase(node);

        // If the node has children, it is not a leaf node, so skip underflow handling for internal nodes
        if (!childNodes.isEmpty()) {
            return;
        }

        // Get the remaining data points in the underflowing node
        List<DataPoint> remainingDataPoints = loadDataPointsFromNode(nodeId);

        // If there are too few data points, delete the leaf node
        if (remainingDataPoints.size() < UNDERFLOW_THRESHOLD) {
            // Delete the underflowing node and its data points from the database
            deleteDataPointsInNode(nodeId);
            deleteNode(nodeId);

            // Reinsert the remaining data points into the tree
            for (DataPoint dataPoint : remainingDataPoints) {
                insertDataPoint(dataPoint);
            }
        }
    }

    private void deleteDataPointsInNode(int nodeId) {
        jdbcTemplate.update("DELETE FROM data_points WHERE node_id = ?", nodeId);
    }

    private Integer findParentIdForNode(int nodeId) {
        System.out.println("we are finding parent id for node id " + nodeId);

        Integer parentId = jdbcTemplate.queryForObject(
                "SELECT parent_id FROM rtree_nodes WHERE id = ?",
                new Object[]{nodeId},
                (rs, rowNum) -> {
                    // Log the result set's value for parent_id
                    Object parentIdValue = rs.getObject("parent_id");
                    if (parentIdValue != null) {
                        System.out.println("Parent ID found: " + parentIdValue + " for node ID: " + nodeId);
                        return rs.getInt("parent_id");
                    } else {
                        System.out.println("No parent ID found (null) for node ID: " + nodeId);
                        return null;
                    }
                }
        );

        return parentId;
    }

    public List<Map<String, Object>> getNearestGeopoints(double lat, double lon, long startTimestamp, long endTimestamp, int limit, int offset) {
        // Convert lat/lon to XYZ coordinates
        double[] queryPointXYZ = coordinateService.convertLatLonToXYZ(lat, lon, 0);
        double queryX = queryPointXYZ[0], queryY = queryPointXYZ[1], queryZ = queryPointXYZ[2];

        // Initialize a priority queue to store nodes sorted by distance
        PriorityQueue<RTreeNode> nodeQueue = new PriorityQueue<>(new DistanceComparator(queryX, queryY, queryZ));
        RTreeNode root = loadRootFromDatabase();
        nodeQueue.add(root);

        List<DataPoint> candidatePoints = new ArrayList<>();

        // Traverse the R-tree to gather potential candidate points
        while (!nodeQueue.isEmpty() && candidatePoints.size() < limit + offset) {
            RTreeNode currentNode = nodeQueue.poll();

            // Check if we have reached a leaf node
            if (loadChildrenFromDatabase(currentNode).isEmpty()) {
                // If it's a leaf, gather data points and filter by timestamp
                List<DataPoint> pointsInNode = loadDataPointsFromNode(currentNode.getId());
                for (DataPoint point : pointsInNode) {
                    if (point.getTimestamp() >= startTimestamp && point.getTimestamp() <= endTimestamp) {
                        candidatePoints.add(point);
                    }
                }
            } else {
                // If it's an internal node, add its children to the queue
                nodeQueue.addAll(loadChildrenFromDatabase(currentNode));
            }
        }

        // Sort candidate points by distance to the query point
        candidatePoints.sort((p1, p2) -> {
            double dist1 = Math.sqrt(Math.pow(p1.getX() - queryX, 2) + Math.pow(p1.getY() - queryY, 2) + Math.pow(p1.getZ() - queryZ, 2));
            double dist2 = Math.sqrt(Math.pow(p2.getX() - queryX, 2) + Math.pow(p2.getY() - queryY, 2) + Math.pow(p2.getZ() - queryZ, 2));
            return Double.compare(dist1, dist2);
        });

        // Apply pagination (offset and limit)
        List<DataPoint> paginatedPoints = candidatePoints.stream()
                .skip(offset)
                .limit(limit)
                .toList();

        // Map the result to a list of maps
        List<Map<String, Object>> results = new ArrayList<>();
        for (DataPoint point : paginatedPoints) {
            Map<String, Object> result = new HashMap<>();
            result.put("id", point.getId());
            result.put("x", point.getX());
            result.put("y", point.getY());
            result.put("z", point.getZ());
            result.put("externalId", point.getExternalId());
            result.put("timestamp", point.getTimestamp());
            double distance = Math.sqrt(Math.pow(point.getX() - queryX, 2) + Math.pow(point.getY() - queryY, 2) + Math.pow(point.getZ() - queryZ, 2));
            result.put("distance", distance);
            results.add(result);
        }

        return results;
    }


}
