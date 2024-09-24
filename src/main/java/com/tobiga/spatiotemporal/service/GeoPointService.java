package com.tobiga.spatiotemporal.service;

import com.tobiga.spatiotemporal.CoordinateService;
import com.tobiga.spatiotemporal.model.AnchorDistance;
import com.tobiga.spatiotemporal.model.AnchorPoint;
import com.tobiga.spatiotemporal.model.GeoPoint;
import com.tobiga.spatiotemporal.model.GeoPointWithDistance;
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

    @Autowired
    private CoordinateService coordinateService;

    private static final List<double[]> ANCHOR_POINTS = List.of(
            new double[] { 1, 0, 0 },
            new double[] { 2, 0, 60 },
            new double[] { 3, 0, 120 },
            new double[] { 4, 0, -60 },
            new double[] { 5, 0, -120 },
            new double[] { 6, 0, 178 },
            new double[] { 7, 0, -178 },

            new double[] { 8, 30, 0 },
            new double[] { 9, 30, 60 },
            new double[] { 10, 30, 120 },
            new double[] { 11, 30, -60 },
            new double[] { 12, 30, -120 },
            new double[] { 13, 30, 178 },
            new double[] { 14, 30, -178 },

            new double[] { 15, 60, 0 },
            new double[] { 16, 60, 60 },
            new double[] { 17, 60, 120 },
            new double[] { 18, 60, -60 },
            new double[] { 19, 60, -120 },
            new double[] { 20, 60, 178 },
            new double[] { 21, 60, -178 },

            new double[] { 22, 85, 0 },
            new double[] { 23, 85, 60 },
            new double[] { 24, 85, 120 },
            new double[] { 25, 85, -60 },
            new double[] { 26, 85, -120 },
            new double[] { 27, 85, 178 },
            new double[] { 28, 85, -178 },

            new double[] { 29, -30, 0 },
            new double[] { 30, -30, 60 },
            new double[] { 31, -30, 120 },
            new double[] { 32, -30, -60 },
            new double[] { 33, -30, -120 },
            new double[] { 34, -30, 178 },
            new double[] { 35, -30, -178 },

            new double[] { 36, -60, 0 },
            new double[] { 37, -60, 60 },
            new double[] { 38, -60, 120 },
            new double[] { 39, -60, -60 },
            new double[] { 40, -60, -120 },
            new double[] { 41, -60, 178 },
            new double[] { 42, -60, -178 },

            new double[] { 43, -85, 0 },
            new double[] { 44, -85, 60 },
            new double[] { 45, -85, 120 },
            new double[] { 46, -85, -60 },
            new double[] { 47, -85, -120 },
            new double[] { 48, -85, 178 },
            new double[] { 49, -85, -178 },

            new double[] { 50, 0, -178 });

    // Helper function to calculate the distance between two points in 3D space
    private double calculateDistance(double[] point1, double[] point2) {
        return Math.sqrt(Math.pow(point1[0] - point2[0], 2) + Math.pow(point1[1] - point2[1], 2) + Math.pow(point1[2] - point2[2], 2));
    }

    // Calculate the distances from the query point to the anchor points and return them as a list of AnchorDistance objects
    public List<AnchorDistance> calculateAnchorDistances(double queryX, double queryY, double queryZ) {
        List<AnchorDistance> anchorDistances = new ArrayList<>();
        double[] queryPoint = { queryX, queryY, queryZ };

        // Loop through the simplified anchor points array
        for (double[] anchor : ANCHOR_POINTS) {
            int anchorId = (int) anchor[0]; // Extract the anchor ID
            double lat = anchor[1]; // Extract latitude
            double lon = anchor[2]; // Extract longitude

            // Convert latitude and longitude to ECEF coordinates
            double[] anchorECEF = coordinateService.convertLatLonToECEF(lat, lon, 0);

            // Calculate the distance from query point to anchor point
            double distance = calculateDistance(queryPoint, anchorECEF);

            // Store the anchor ID and calculated distance
            anchorDistances.add(new AnchorDistance(anchorId, distance));
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

    public List<GeoPointWithDistance> findGeoPointsNearQueryPoint(double queryX, double queryY, double queryZ, Instant start, Instant end, int limit, int offset) {
        List<AnchorDistance> anchorDistances = calculateAnchorDistances(queryX, queryY, queryZ);
        List<Double> distanceValues = new ArrayList<>();
        for (AnchorDistance ad : anchorDistances) {
            distanceValues.add(ad.getAnchorDistance()); // Only get the distance values for the query
        }

        List<GeoPoint> geoPoints = geoPointRepository.findGeoPointsNearQueryPoint(distanceValues, start, end, limit, offset);

        double[] queryPoint = { queryX, queryY, queryZ };

        List<GeoPointWithDistance> geoPointsWithDistance = new ArrayList<>();

        for (GeoPoint geoPoint : geoPoints) {
            double[] geoPointCoordinates = { geoPoint.getX(), geoPoint.getY(), geoPoint.getZ() };
            double distance = calculateDistance(queryPoint, geoPointCoordinates);
            geoPointsWithDistance.add(new GeoPointWithDistance(geoPoint, distance));
        }

        return geoPointsWithDistance;
    }

    public List<GeoPoint> getPointsWithinTimeRange(Instant start, Instant end) {
        return geoPointRepository.findByTimestampBetween(start, end);
    }
}
