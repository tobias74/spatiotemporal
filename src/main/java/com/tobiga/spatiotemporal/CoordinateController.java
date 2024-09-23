package com.tobiga.spatiotemporal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coordinates")
public class CoordinateController {

    @Autowired
    private CoordinateService coordinateService;

    // Convert lat/lon/alt to ECEF (x, y, z)
    @GetMapping("/convertLatLonToECEF")
    public ResponseEntity<double[]> convertLatLonToECEF(
            @RequestParam(name = "lat") double lat,
            @RequestParam(name = "lon") double lon,
            @RequestParam(name = "alt", defaultValue = "0") double alt) {
        double[] ecefCoords = coordinateService.convertLatLonToECEF(lat, lon, alt);
        return ResponseEntity.ok(ecefCoords);
    }

    // Convert ECEF (x, y, z) to lat/lon
    @GetMapping("/convertECEFToLatLon")
    public ResponseEntity<double[]> convertECEFToLatLon(
            @RequestParam(name = "x") double x,
            @RequestParam(name = "y") double y,
            @RequestParam(name = "z") double z) {
        double[] latLonCoords = coordinateService.convertECEFToLatLon(x, y, z);
        return ResponseEntity.ok(latLonCoords);
    }
}
