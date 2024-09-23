package com.tobiga.spatiotemporal.service;

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

    // Predefined anchor points in ECEF (x, y, z) coordinates
    private static final double[][] ANCHOR_POINTS = {
            { 0, 6371000, 0 }, // Anchor 1 (on the equator)
            { 0, -6371000, 0 }, // Anchor 2 (opposite equator)
            { 6371000, 0, 0 }, // Anchor 3
            { -6371000, 0, 0 }, // Anchor 4
            { 0, 0, 6371000 }, // Anchor 5 (North Pole)
            { 0, 0, -6371000 }, // Anchor 6 (South Pole)
            { 4500000, 4500000, 0 }, // Anchor 7
            { -4500000, -4500000, 0 }, // Anchor 8
            { 4500000, 0, 4500000 }, // Anchor 9
            { 0, 4500000, -4500000 } // Anchor 10
    };

    // Helper function to calculate the distance between two points in 3D space
    private double calculateDistance(double[] point1, double[] point2) {
        return Math.sqrt(Math.pow(point1[0] - point2[0], 2) + Math.pow(point1[1] - point2[1], 2) + Math.pow(point1[2] - point2[2], 2));
    }

    // Save a GeoPoint with pre-calculated anchor distances
    public GeoPoint saveGeoPoint(double x, double y, double z, Instant timestamp) {
        double[] currentPoint = { x, y, z };
        List<Double> anchorDistances = new ArrayList<>();

        // Calculate distances to all 10 anchor points and store them in the list
        for (double[] anchor : ANCHOR_POINTS) {
            anchorDistances.add(calculateDistance(currentPoint, anchor));
        }

        // Save the GeoPoint with the anchor distances
        GeoPoint geoPoint = new GeoPoint(x, y, z, timestamp, anchorDistances);
        return geoPointRepository.save(geoPoint);
    }

    public List<GeoPoint> getPointsWithinTimeRange(Instant start, Instant end) {
        return geoPointRepository.findByTimestampBetween(start, end);
    }
}
