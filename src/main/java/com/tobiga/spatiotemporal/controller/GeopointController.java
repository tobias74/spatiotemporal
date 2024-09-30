package com.tobiga.spatiotemporal.controller;

import com.tobiga.spatiotemporal.service.RTreeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tobiga.spatiotemporal.service.CoordinateService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/geopoints")
public class GeopointController {

    private final RTreeService rTreeService;
    private final CoordinateService coordinateService;

    public GeopointController(RTreeService RTreeService, CoordinateService coordinateService) {
        this.rTreeService = RTreeService;
        this.coordinateService = coordinateService;
    }

    // API to insert a new geopoint with lat/lon/alt
    @PostMapping("/insert")
    public ResponseEntity<String> insertGeopoint(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam long timestamp,
            @RequestParam String externalId) {

        try {
            // Call the correct method in SQLiteService to insert geopoint
            rTreeService.insertGeopointWithLatLon(lat, lon, externalId, timestamp);
            return ResponseEntity.ok("Geopoint inserted successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error inserting geopoint.");
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteGeopoint(@RequestParam String externalId) {
        try {
            rTreeService.deleteGeopointByExternalId(externalId);
            return ResponseEntity.ok("Geopoint deleted successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error deleting geopoint.");
        }
    }

    @GetMapping("/nearest")
    public ResponseEntity<List<Map<String, Object>>> getNearestGeopoints(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam long startTimestamp,
            @RequestParam long endTimestamp,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        try {
            // Query for nearest geopoints
            List<Map<String, Object>> nearestGeopoints = rTreeService.getNearestGeopoints(lat, lon, startTimestamp, endTimestamp, limit, offset);

            // Iterate through the results and add the distance using the Haversine formula
            for (Map<String, Object> geopoint : nearestGeopoints) {
                double pointX = (double) geopoint.get("x");
                double pointY = (double) geopoint.get("y");
                double pointZ = (double) geopoint.get("z");

                // Convert XYZ coordinates back to latitude and longitude
                double[] latLonAlt = coordinateService.convertXYZToLatLon(pointX, pointY, pointZ);
                double pointLat = latLonAlt[0];
                double pointLon = latLonAlt[1];

                // Calculate the distance using the Haversine formula
                double distance = coordinateService.haversine(lat, lon, pointLat, pointLon);

                // Add the calculated distance to the geopoint result
                geopoint.put("haversine_distance", distance);
                geopoint.put("latitude", pointLat);
                geopoint.put("longitude", pointLon);

                geopoint.remove("id");
            }

            return ResponseEntity.ok(nearestGeopoints);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    /*
    @GetMapping("/query")
    public ResponseEntity<List<Map<String, Object>>> queryGeopoints(GeopointQuery geopointQuery) {

        try {
            List<Map<String, Object>> geopoints = RTreeService.queryGeopointsByDistanceAndTime(geopointQuery);
            return ResponseEntity.ok(geopoints);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }
    */
}
