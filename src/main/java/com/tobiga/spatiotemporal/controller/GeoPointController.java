package com.tobiga.spatiotemporal.controller;

import com.tobiga.spatiotemporal.dto.GeoPointRequest;
import com.tobiga.spatiotemporal.model.GeoPoint;
import com.tobiga.spatiotemporal.service.GeoPointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Random;

@RestController @RequestMapping("/api/geo-points")
public class GeoPointController {

    @Autowired
    private GeoPointService geoPointService;

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
    public List<GeoPoint> queryGeoPoints(
            @RequestParam(name = "x") double queryX,
            @RequestParam(name = "y") double queryY,
            @RequestParam(name = "z") double queryZ,
            @RequestParam(name = "tolerance", required = false, defaultValue = "50") double tolerance,
            @RequestParam(name = "start") String startTimestamp,
            @RequestParam(name = "end") String endTimestamp,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit,
            @RequestParam(name = "offset", required = false, defaultValue = "0") int offset) {

        // Parse the start and end timestamps
        Instant start = Instant.parse(startTimestamp);
        Instant end = Instant.parse(endTimestamp);

        // Call the service method to handle the query point, tolerance, and time range
        return geoPointService.findGeoPointsNearQueryPoint(queryX, queryY, queryZ, start, end, limit, offset);
    }

    @PostMapping("/generate-random-points")
    public String generateRandomPoints(@RequestParam(name = "count") int count) {
        Random random = new Random();

        for (int i = 0; i < count; i++) {
            // Generate random coordinates (x, y, z) within Earth's range
            double x = random.nextDouble() * 12742000 - 6371000; // Random x coordinate (approx Earth radius)
            double y = random.nextDouble() * 12742000 - 6371000; // Random y coordinate (approx Earth radius)
            double z = random.nextDouble() * 12742000 - 6371000; // Random z coordinate (approx Earth radius)

            // Generate a random timestamp (within the last 10 years)
            Instant timestamp = Instant.now().minusSeconds(random.nextInt(10 * 365 * 24 * 60 * 60));

            // Save the random GeoPoint
            geoPointService.saveGeoPoint(x, y, z, timestamp);
        }

        return count + " random points have been added to the database.";
    }
}
