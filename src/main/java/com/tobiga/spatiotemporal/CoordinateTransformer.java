package com.tobiga.spatiotemporal;

public class CoordinateTransformer {

    private static final double EARTH_RADIUS = 6371000; // Earth's radius in meters

    // Convert lat/lon/alt to ECEF (x, y, z)
    public static double[] latLonToECEF(double lat, double lon, double alt) {
        double latRad = Math.toRadians(lat);
        double lonRad = Math.toRadians(lon);

        double x = (EARTH_RADIUS + alt) * Math.cos(latRad) * Math.cos(lonRad);
        double y = (EARTH_RADIUS + alt) * Math.cos(latRad) * Math.sin(lonRad);
        double z = (EARTH_RADIUS + alt) * Math.sin(latRad);

        return new double[] { x, y, z };
    }

    // Convert ECEF (x, y, z) to lat/lon/alt
    public static double[] ecefToLatLon(double x, double y, double z) {
        double lon = Math.toDegrees(Math.atan2(y, x));
        double hyp = Math.sqrt(x * x + y * y); // Distance to the z-axis
        double lat = Math.toDegrees(Math.atan2(z, hyp));

        return new double[] { lat, lon };
    }
}