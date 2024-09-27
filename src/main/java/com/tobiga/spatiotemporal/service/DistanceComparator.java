package com.tobiga.spatiotemporal.service;

import java.util.Comparator;

public class DistanceComparator implements Comparator<RTreeNode> {
    private final Coordinate queryPoint;

    public DistanceComparator(Coordinate queryPoint) {
        this.queryPoint = queryPoint;
    }

    @Override
    public int compare(RTreeNode node1, RTreeNode node2) {
        double dist1 = node1.getBoundingBox().distanceTo(queryPoint);
        double dist2 = node2.getBoundingBox().distanceTo(queryPoint);
        return Double.compare(dist1, dist2);
    }
}
