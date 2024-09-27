package com.tobiga.spatiotemporal.controller;

import com.tobiga.spatiotemporal.service.RTreeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tobiga.spatiotemporal.dto.GeopointQuery;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/geopoints")
public class GeopointController {

    private final RTreeService RTreeService;

    public GeopointController(RTreeService RTreeService) {
        this.RTreeService = RTreeService;
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
            RTreeService.insertGeopointWithLatLon(lat, lon, externalId, timestamp);
            return ResponseEntity.ok("Geopoint inserted successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error inserting geopoint.");
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
