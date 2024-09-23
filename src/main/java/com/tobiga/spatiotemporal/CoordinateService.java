package com.tobiga.spatiotemporal;

import org.springframework.stereotype.Service;

@Service
public class CoordinateService {

    // Convert lat/lon to ECEF (x, y, z)
    public double[] convertLatLonToECEF(double lat, double lon, double alt) {
        return CoordinateTransformer.latLonToECEF(lat, lon, alt);
    }

    // Convert ECEF (x, y, z) to lat/lon
    public double[] convertECEFToLatLon(double x, double y, double z) {
        return CoordinateTransformer.ecefToLatLon(x, y, z);
    }
}