package com.tobiga.spatiotemporal.service;

import com.tobiga.spatiotemporal.model.AnchorDistance;
import com.tobiga.spatiotemporal.model.AnchorPoint;
import com.tobiga.spatiotemporal.model.GeoPoint;
import com.tobiga.spatiotemporal.repository.GeoPointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeoPointService {

    @Autowired
    private GeoPointRepository geoPointRepository;

    // Predefined anchor points with explicit anchor IDs and 3D coordinates
    private static final List<AnchorPoint> ANCHOR_POINTS = List.of(
            new AnchorPoint(1, new double[] { 0, 6371000, 0 }), // Anchor 1 (on the equator)
            new AnchorPoint(2, new double[] { 0, -6371000, 0 }), // Anchor 2 (opposite equator)
            new AnchorPoint(3, new double[] { 6371000, 0, 0 }), // Anchor 3
            new AnchorPoint(4, new double[] { -6371000, 0, 0 }), // Anchor 4
            new AnchorPoint(5, new double[] { 0, 0, 6371000 }), // Anchor 5 (North Pole)
            new AnchorPoint(6, new double[] { 0, 0, -6371000 }), // Anchor 6 (South Pole)
            new AnchorPoint(7, new double[] { 4500000, 4500000, 0 }), // Anchor 7
            new AnchorPoint(8, new double[] { -4500000, -4500000, 0 }), // Anchor 8
            new AnchorPoint(9, new double[] { 4500000, 0, 4500000 }), // Anchor 9
            new AnchorPoint(10, new double[] { 0, 4500000, -4500000 }) // Anchor 10
    );

    // Helper function to calculate the distance between two points in 3D space
    private double calculateDistance(double[] point1, double[] point2) {
        return Math.sqrt(Math.pow(point1[0] - point2[0], 2) + Math.pow(point1[1] - point2[1], 2) + Math.pow(point1[2] - point2[2], 2));
    }

    // Calculate the distances from the query point to the anchor points and return them as a list of AnchorDistance objects
    public List<AnchorDistance> calculateAnchorDistances(double queryX, double queryY, double queryZ) {
        List<AnchorDistance> anchorDistances = new ArrayList<>();
        double[] queryPoint = { queryX, queryY, queryZ };

        for (AnchorPoint anchor : ANCHOR_POINTS) {
            double distance = calculateDistance(queryPoint, anchor.getCoordinates());
            anchorDistances.add(new AnchorDistance(anchor.getAnchorId(), distance)); // Use explicit anchorId
        }
        return anchorDistances;
    }

    // Save a GeoPoint with pre-calculated anchor distances
    public GeoPoint saveGeoPoint(double x, double y, double z, Instant timestamp) {
        List<AnchorDistance> anchorDistances = calculateAnchorDistances(x, y, z);

        // Save the GeoPoint with the anchor distances
        GeoPoint geoPoint = new GeoPoint(x, y, z, timestamp, anchorDistances);
        return geoPointRepository.save(geoPoint);
    }

    // This method takes the query point, calculates the distances to the anchors, and calls the repository
    public List<GeoPoint> findGeoPointsNearQueryPoint(double queryX, double queryY, double queryZ, Instant start, Instant end, int limit, int offset) {
        List<AnchorDistance> anchorDistances = calculateAnchorDistances(queryX, queryY, queryZ);
        List<Double> distanceValues = new ArrayList<>();
        for (AnchorDistance ad : anchorDistances) {
            distanceValues.add(ad.getAnchorDistance()); // Only get the distance values for the query
        }
        return geoPointRepository.findGeoPointsNearQueryPoint(distanceValues, start, end, limit, offset);
    }

    public List<GeoPoint> getPointsWithinTimeRange(Instant start, Instant end) {
        return geoPointRepository.findByTimestampBetween(start, end);
    }
}
