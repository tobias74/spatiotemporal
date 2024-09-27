package com.tobiga.spatiotemporal.service;

import java.util.List;

public class RTreeNode {
    private BoundingBox boundingBox;
    private List<RTreeNode> children;
    private List<DataPoint> dataPoints;  // Only for leaf nodes
    private boolean isLeaf;

    public RTreeNode(BoundingBox boundingBox, boolean isLeaf) {
        this.boundingBox = boundingBox;
        this.isLeaf = isLeaf;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public List<RTreeNode> getChildren() {
        return children;
    }

    public List<DataPoint> getDataPoints() {
        return dataPoints;
    }

    // Add methods for adding children, inserting data, etc.
}
