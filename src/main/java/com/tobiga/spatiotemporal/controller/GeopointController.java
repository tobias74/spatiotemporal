package com.tobiga.spatiotemporal.controller;

import com.tobiga.spatiotemporal.service.SQLiteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/geopoints")
public class GeopointController {

    private final SQLiteService sqLiteService;

    public GeopointController(SQLiteService sqLiteService) {
        this.sqLiteService = sqLiteService;
    }

    // API to insert a new geopoint with lat/lon/alt
    @PostMapping("/insert")
    public ResponseEntity<String> insertGeopoint(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam String externalId) {

        try {
            // Call the correct method in SQLiteService to insert geopoint
            sqLiteService.insertGeopointWithLatLon(lat, lon, externalId);
            return ResponseEntity.ok("Geopoint inserted successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error inserting geopoint.");
        }
    }
}
