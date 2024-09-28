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
    public List<RTreeNode> splitNode(RTreeNode node) {
        // Use the RTreeService to load data points from the database
        List<DataPoint> dataPoints = rTreeService.loadDataPointsFromNode(node.getId());

        // Step 1: Choose two seeds that will start the two groups
        Pair<DataPoint, DataPoint> seeds = chooseSeeds(dataPoints);

        // Create two new nodes initialized with the seeds
        RTreeNode newNode1 = new RTreeNode(rTreeService.generateNewNodeId(), node.getId(), new BoundingBox(seeds.getLeft()), true, false);
        RTreeNode newNode2 = new RTreeNode(rTreeService.generateNewNodeId(), node.getId(), new BoundingBox(seeds.getRight()), true, false);

        // Insert the seed points into the new nodes
        rTreeService.insertDataPointIntoNode(newNode1, seeds.getLeft());
        rTreeService.insertDataPointIntoNode(newNode2, seeds.getRight());

        // Step 2: Assign the remaining data points to one of the two new nodes
        List<DataPoint> remainingPoints = new ArrayList<>(dataPoints);
        remainingPoints.remove(seeds.getLeft());
        remainingPoints.remove(seeds.getRight());

        for (DataPoint point : remainingPoints) {
            // Calculate the enlargement needed for both nodes if the point is added
            double enlargement1 = newNode1.getBoundingBox().enlargementNeeded(point);
            double enlargement2 = newNode2.getBoundingBox().enlargementNeeded(point);

            // Assign the point to the node that requires the least enlargement
            if (enlargement1 < enlargement2) {
                rTreeService.insertDataPointIntoNode(newNode1, point);
            } else {
                rTreeService.insertDataPointIntoNode(newNode2, point);
            }
        }

        // Return the two new nodes
        return List.of(newNode1, newNode2);
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
