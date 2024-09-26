package com.tobiga.spatiotemporal.service;

import org.springframework.stereotype.Service;

@Service
public class CoordinateService {

    private static final double EARTH_RADIUS = 6371000; // Earth's radius in meters

    // Convert lat/lon/alt to ECEF (X, Y, Z) coordinates
    public double[] convertLatLonToXYZ(double lat, double lon, double alt) {
        double latRad = Math.toRadians(lat);
        double lonRad = Math.toRadians(lon);

        double x = (EARTH_RADIUS + alt) * Math.cos(latRad) * Math.cos(lonRad);
        double y = (EARTH_RADIUS + alt) * Math.cos(latRad) * Math.sin(lonRad);
        double z = (EARTH_RADIUS + alt) * Math.sin(latRad);

        return new double[] { x, y, z };
    }

    // Convert ECEF (X, Y, Z) to lat/lon/alt
    public double[] convertXYZToLatLon(double x, double y, double z) {
        double lon = Math.toDegrees(Math.atan2(y, x));
        double hyp = Math.sqrt(x * x + y * y); // Hypotenuse distance from the Z-axis
        double lat = Math.toDegrees(Math.atan2(z, hyp));
        double alt = Math.sqrt(x * x + y * y + z * z) - EARTH_RADIUS;

        return new double[] { lat, lon, alt };
    }
}
