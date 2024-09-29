package com.tobiga.spatiotemporal.service;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
                result.addAll(loadDataPointsFromNode(currentNode.getId())); // Add points from leaf
            } else {
                nodeQueue.addAll(loadChildrenFromDatabase(currentNode)); // Traverse children
            }
        }
        return result;
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
                    rs.getBoolean("isLeaf"),
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

                    // Return the RTreeNode with the isRoot property and parentId set correctly
                    return new RTreeNode(
                            nodeId,
                            rs.getObject("parent_id", Integer.class),  // Load the parent_id (can be null for the root)
                            new BoundingBox(rs.getDouble("minX"), rs.getDouble("maxX"),
                                    rs.getDouble("minY"), rs.getDouble("maxY"),
                                    rs.getDouble("minZ"), rs.getDouble("maxZ")),
                            rs.getBoolean("isLeaf"),
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

        String sql = "INSERT INTO rtree_nodes (parent_id, minX, maxX, minY, maxY, minZ, maxZ, isLeaf) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setObject(1, node.getParentId());  // parent_id can be null
            ps.setDouble(2, node.getBoundingBox().getMinX());
            ps.setDouble(3, node.getBoundingBox().getMaxX());
            ps.setDouble(4, node.getBoundingBox().getMinY());
            ps.setDouble(5, node.getBoundingBox().getMaxY());
            ps.setDouble(6, node.getBoundingBox().getMinZ());
            ps.setDouble(7, node.getBoundingBox().getMaxZ());
            ps.setBoolean(8, node.isLeaf());
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

    public void insertNodesIntoParent(RTreeNode parentNode, List<RTreeNode> newNodes) {
        for (RTreeNode newNode : newNodes) {
            KeyHolder keyHolder = new GeneratedKeyHolder();

            String sql = "INSERT INTO rtree_nodes (parent_id, minX, maxX, minY, maxY, minZ, maxZ, isLeaf) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
                ps.setObject(1, parentNode.getId());  // Set the correct parent ID
                ps.setDouble(2, newNode.getBoundingBox().getMinX());
                ps.setDouble(3, newNode.getBoundingBox().getMaxX());
                ps.setDouble(4, newNode.getBoundingBox().getMinY());
                ps.setDouble(5, newNode.getBoundingBox().getMaxY());
                ps.setDouble(6, newNode.getBoundingBox().getMinZ());
                ps.setDouble(7, newNode.getBoundingBox().getMaxZ());
                ps.setBoolean(8, newNode.isLeaf());
                return ps;
            }, keyHolder);

            // Assign the generated ID to the newNode
            int generatedId = keyHolder.getKey().intValue();
            newNode.setId(generatedId);

            System.out.println("New node inserted with ID: " + generatedId);
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
        RTreeNode newRoot = new RTreeNode(null, null, newRootBoundingBox, false, true);

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


    private RTreeNode createNewRoot(RTreeNode child1, RTreeNode child2) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        BoundingBox newRootBoundingBox = BoundingBox.combine(child1.getBoundingBox(), child2.getBoundingBox());

        String sql = "INSERT INTO rtree_nodes (parent_id, minX, maxX, minY, maxY, minZ, maxZ, isLeaf) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        // Insert the new root node into the database
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setNull(1, java.sql.Types.INTEGER);  // Root node has no parent, so parent_id is null
            ps.setDouble(2, newRootBoundingBox.getMinX());
            ps.setDouble(3, newRootBoundingBox.getMaxX());
            ps.setDouble(4, newRootBoundingBox.getMinY());
            ps.setDouble(5, newRootBoundingBox.getMaxY());
            ps.setDouble(6, newRootBoundingBox.getMinZ());
            ps.setDouble(7, newRootBoundingBox.getMaxZ());
            ps.setBoolean(8, false);  // Root is not a leaf
            return ps;
        }, keyHolder);

        // Retrieve the generated ID
        int newRootId = keyHolder.getKey().intValue();

        // Create the new root node with the assigned ID
        RTreeNode newRoot = new RTreeNode(newRootId, null, newRootBoundingBox, false, true);

        // Update the child nodes with the new root ID
        jdbcTemplate.update("UPDATE rtree_nodes SET parent_id = ? WHERE id = ?", newRootId, child1.getId());
        jdbcTemplate.update("UPDATE rtree_nodes SET parent_id = ? WHERE id = ?", newRootId, child2.getId());

        return newRoot;
    }

    public void insertDataPointIntoNode(RTreeNode node, DataPoint point) {
        // Insert the data point into the database, associating it with the node
        String query = "INSERT INTO data_points (node_id, x, y, z, externalId, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(query, node.getId(), point.getX(), point.getY(), point.getZ(), point.getExternalId(), point.getTimestamp());

        // Optionally, you can also update the bounding box of the node if necessary
        updateBoundingBoxAfterInsert(node, point);
    }

    public List<DataPoint> loadDataPointsFromNode(int nodeId) {
        String query = "SELECT * FROM data_points WHERE node_id = ?";
        return jdbcTemplate.query(query, new Object[]{nodeId}, (rs, rowNum) ->
                new DataPoint(
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


    private void updateBoundingBoxAfterInsert(RTreeNode node, DataPoint point) {
        // Update the bounding box of the node if the new point lies outside the current bounding box
        BoundingBox currentBoundingBox = node.getBoundingBox();

        // Check if the point extends the bounding box and adjust it accordingly
        double newMinX = Math.min(currentBoundingBox.getMinX(), point.getX());
        double newMaxX = Math.max(currentBoundingBox.getMaxX(), point.getX());
        double newMinY = Math.min(currentBoundingBox.getMinY(), point.getY());
        double newMaxY = Math.max(currentBoundingBox.getMaxY(), point.getY());
        double newMinZ = Math.min(currentBoundingBox.getMinZ(), point.getZ());
        double newMaxZ = Math.max(currentBoundingBox.getMaxZ(), point.getZ());

        // If the bounding box needs to be updated, update it in the database
        if (newMinX != currentBoundingBox.getMinX() || newMaxX != currentBoundingBox.getMaxX() ||
                newMinY != currentBoundingBox.getMinY() || newMaxY != currentBoundingBox.getMaxY() ||
                newMinZ != currentBoundingBox.getMinZ() || newMaxZ != currentBoundingBox.getMaxZ()) {

            // Update the node's bounding box in the database
            jdbcTemplate.update("UPDATE rtree_nodes SET minX = ?, maxX = ?, minY = ?, maxY = ?, minZ = ?, maxZ = ? WHERE id = ?",
                    newMinX, newMaxX, newMinY, newMaxY, newMinZ, newMaxZ, node.getId());
        }
    }

}
