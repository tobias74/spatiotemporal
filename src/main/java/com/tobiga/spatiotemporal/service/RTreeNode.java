package com.tobiga.spatiotemporal.service;

import java.util.List;

public class RTreeNode {
    private int id;  // Add an id field to identify the node
    private BoundingBox boundingBox;
    private List<RTreeNode> children;
    private List<DataPoint> dataPoints;  // Leaf node data
    private boolean isLeaf;

    public RTreeNode(int id, BoundingBox boundingBox, boolean isLeaf) {
        this.id = id;
        this.boundingBox = boundingBox;
        this.isLeaf = isLeaf;
    }

    // Getter and setter for id
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public List<RTreeNode> getChildren() {
        return children;
    }

    public List<DataPoint> getDataPoints() {
        return dataPoints;
    }

    // Add methods for adding children, inserting data, etc.
}
