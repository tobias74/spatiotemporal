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
            { 0, 6371000, 0 }, // Anchor 1
            { 0, -6371000, 0 }, // Anchor 2
            // Add the rest of your 10 anchor points here...
    };

    // Helper function to calculate distance between two points in 3D space
    private double calculateDistance(double[] point1, double[] point2) {
        return Math.sqrt(Math.pow(point1[0] - point2[0], 2) +
                Math.pow(point1[1] - point2[1], 2) +
                Math.pow(point1[2] - point2[2], 2));
    }

    // Save GeoPoint with pre-calculated anchor distances
    public GeoPoint saveGeoPoint(double x, double y, double z, Instant timestamp) {
        double[] currentPoint = { x, y, z };
        List<Double> anchorDistances = new ArrayList<>();

        // Calculate distances to all 10 anchor points and store them in a list
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
