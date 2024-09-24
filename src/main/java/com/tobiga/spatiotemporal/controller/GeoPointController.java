package com.tobiga.spatiotemporal.controller;

import com.tobiga.spatiotemporal.CoordinateService;
import com.tobiga.spatiotemporal.dto.GeoPointRequest;
import com.tobiga.spatiotemporal.dto.GeoPointResult;
import com.tobiga.spatiotemporal.model.GeoPoint;
import com.tobiga.spatiotemporal.model.GeoPointWithDistance;
import com.tobiga.spatiotemporal.service.GeoPointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;

@RestController @RequestMapping("/api/geo-points")
public class GeoPointController {

    @Autowired
    private GeoPointService geoPointService;

    @Autowired
    private CoordinateService coordinateService;

    // Accept JSON data
    @PostMapping("/save")
    public ResponseEntity<GeoPoint> saveGeoPoint(@RequestBody GeoPointRequest request) {
        GeoPoint savedPoint = geoPointService.saveGeoPoint(
                request.getX(), request.getY(), request.getZ(), request.getTimestamp());
        return ResponseEntity.ok(savedPoint);
    }

    @GetMapping("/by-time-range")
    public ResponseEntity<List<GeoPoint>> getPointsByTimeRange(@RequestParam Instant start, @RequestParam Instant end) {
        List<GeoPoint> points = geoPointService.getPointsWithinTimeRange(start, end);
        return ResponseEntity.ok(points);
    }

    @GetMapping("/query")
    public List<GeoPointResult> queryGeoPoints(
            @RequestParam(name = "latitude") double latitude,
            @RequestParam(name = "longitude") double longitude,
            @RequestParam(name = "start") String startTimestamp,
            @RequestParam(name = "end") String endTimestamp,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit,
            @RequestParam(name = "offset", required = false, defaultValue = "0") int offset) {

        // Step 1: Convert latitude and longitude to ECEF (x, y, z)
        double[] ecefCoordinates = coordinateService.convertLatLonToECEF(latitude, longitude, 0); // Altitude is 0 for surface points
        double queryX = ecefCoordinates[0];
        double queryY = ecefCoordinates[1];
        double queryZ = ecefCoordinates[2];

        // Step 2: Parse the start and end timestamps
        Instant start = Instant.parse(startTimestamp);
        Instant end = Instant.parse(endTimestamp);

        // Step 3: Get the geo points with distances
        List<GeoPointWithDistance> geoPointsWithDistance = geoPointService.findGeoPointsNearQueryPoint(queryX, queryY, queryZ, start, end, limit, offset);

        // Step 4: Convert the result to GeoPointResult (id, lat, lon, timestamp, and distance)
        List<GeoPointResult> geoPointResults = new ArrayList<>();
        for (GeoPointWithDistance geoPointWithDistance : geoPointsWithDistance) {
            GeoPoint geoPoint = geoPointWithDistance.getGeoPoint();

            // Convert ECEF back to lat/lon
            double[] latLon = coordinateService.convertECEFToLatLon(geoPoint.getX(), geoPoint.getY(), geoPoint.getZ());
            double resultLat = latLon[0];
            double resultLon = latLon[1];

            // Create a GeoPointResult object with ID, latitude, longitude, timestamp, and distance
            GeoPointResult result = new GeoPointResult(geoPoint.getId(), resultLat, resultLon, geoPoint.getTimestamp(), geoPointWithDistance.getDistanceToQueryPoint());
            geoPointResults.add(result);
        }

        return geoPointResults;
    }

    @PostMapping("/generate-random-points")
    public String generateRandomPoints(@RequestParam(name = "count") int count) {
        Random random = new Random();

        for (int i = 0; i < count; i++) {
            // Step 1: Generate random latitude and longitude
            double latitude = -90 + random.nextDouble() * 180; // Latitude between -90 and 90 degrees
            double longitude = -180 + random.nextDouble() * 360; // Longitude between -180 and 180 degrees

            // Step 2: Use CoordinateService to convert lat/lon to ECEF (x, y, z)
            double[] ecefCoordinates = coordinateService.convertLatLonToECEF(latitude, longitude, 0); // Altitude is 0 for surface

            double x = ecefCoordinates[0];
            double y = ecefCoordinates[1];
            double z = ecefCoordinates[2];

            // Step 3: Generate a random timestamp (within the last 10 years)
            Instant timestamp = Instant.now().minusSeconds(random.nextInt(10 * 365 * 24 * 60 * 60));

            // Step 4: Save the random GeoPoint
            geoPointService.saveGeoPoint(x, y, z, timestamp);
        }

        return count + " random points on the surface of the globe have been added to the database.";
    }
}
