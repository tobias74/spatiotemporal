package com.tobiga.spatiotemporal.service;

import java.util.List;

public class RTreeNode {
    private Integer id;  // Add an id field to identify the node
    private BoundingBox boundingBox;
    private boolean isLeaf;
    private boolean isRoot;
    private Integer parentId;

    public RTreeNode(Integer id, Integer parentId, BoundingBox boundingBox, boolean isLeaf, boolean isRoot) {
        this.id = id;
        this.parentId = parentId;  // Set the parent ID
        this.boundingBox = boundingBox;
        this.isLeaf = isLeaf;
        this.isRoot = isRoot;
    }

    // Getter and setter for id
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getParentId() {
        return parentId;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public void setRoot(boolean isRoot) {
        this.isRoot = isRoot;
    }

    // Add methods for adding children, inserting data, etc.
}
