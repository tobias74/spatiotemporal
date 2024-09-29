package com.tobiga.spatiotemporal.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;

@Component
public class QuadraticSplitStrategy implements SplitStrategy {

    private final RTreeService rTreeService;

    @Autowired
    public QuadraticSplitStrategy(RTreeService rTreeService) {
        this.rTreeService = rTreeService;
    }

    @Override
    public void splitNode(RTreeNode node) {
        List<DataPoint> dataPoints = rTreeService.loadDataPointsFromNode(node.getId());

        // Step 1: Choose two seeds that will start the two groups
        Pair<DataPoint, DataPoint> seeds = chooseSeeds(dataPoints);

        // Create two new child nodes (without ID initially)
        RTreeNode newNode1 = new RTreeNode(null, node.getId(), new BoundingBox(seeds.getLeft()), false);
        RTreeNode newNode2 = new RTreeNode(null, node.getId(), new BoundingBox(seeds.getRight()), false);

        // Insert the nodes and get their IDs updated
        rTreeService.insertNewNode(newNode1);
        rTreeService.insertNewNode(newNode2);

        // Step 2: Update the data points to reflect their new parent node
        rTreeService.updateDataPointNodeId(newNode1.getId(), seeds.getLeft());
        rTreeService.updateDataPointNodeId(newNode2.getId(), seeds.getRight());

        // Step 3: Process remaining points and update their node IDs
        List<DataPoint> remainingPoints = new ArrayList<>(dataPoints);
        remainingPoints.remove(seeds.getLeft());
        remainingPoints.remove(seeds.getRight());

        for (DataPoint point : remainingPoints) {
            double enlargement1 = newNode1.getBoundingBox().enlargementNeeded(point);
            double enlargement2 = newNode2.getBoundingBox().enlargementNeeded(point);

            if (enlargement1 < enlargement2) {
                rTreeService.updateDataPointNodeId(newNode1.getId(), point);
            } else {
                rTreeService.updateDataPointNodeId(newNode2.getId(), point);
            }
        }

        // Check if the current node is the root
        if (node.isRoot()) {
            rTreeService.handleRootSplit(newNode1, newNode2, node);  // Handle root-specific behavior
        }

    }

    private Pair<DataPoint, DataPoint> chooseSeeds(List<DataPoint> dataPoints) {
        double maxDistance = Double.NEGATIVE_INFINITY;
        DataPoint seed1 = null;
        DataPoint seed2 = null;

        // Step through all pairs of points to find the most distant pair
        for (int i = 0; i < dataPoints.size(); i++) {
            for (int j = i + 1; j < dataPoints.size(); j++) {
                double distance = calculateDistance(dataPoints.get(i), dataPoints.get(j));
                if (distance > maxDistance) {
                    maxDistance = distance;
                    seed1 = dataPoints.get(i);
                    seed2 = dataPoints.get(j);
                }
            }
        }

        return new Pair<>(seed1, seed2);
    }

    private double calculateDistance(DataPoint p1, DataPoint p2) {
        return Math.sqrt(Math.pow(p1.getX() - p2.getX(), 2) +
                Math.pow(p1.getY() - p2.getY(), 2) +
                Math.pow(p1.getZ() - p2.getZ(), 2));
    }
}
