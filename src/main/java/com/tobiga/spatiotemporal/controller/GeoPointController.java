package com.tobiga.spatiotemporal.controller;

import com.tobiga.spatiotemporal.dto.GeoPointRequest;
import com.tobiga.spatiotemporal.model.GeoPoint;
import com.tobiga.spatiotemporal.service.GeoPointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

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
            @RequestParam(name = "tolerance") double tolerance,
            @RequestParam(name = "start") String startTimestamp,
            @RequestParam(name = "end") String endTimestamp) {

        // Parse the start and end timestamps from the request
        Instant start = Instant.parse(startTimestamp);
        Instant end = Instant.parse(endTimestamp);

        // Call the service method to handle the query point, tolerance, and time range
        return geoPointService.findGeoPointsNearQueryPoint(queryX, queryY, queryZ, tolerance, start, end);
    }
}
