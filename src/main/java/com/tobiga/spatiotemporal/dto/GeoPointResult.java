package com.tobiga.spatiotemporal.dto;

import java.time.Instant;

public class GeoPointResult {
    private Long id;
    private double latitude;
    private double longitude;
    private Instant timestamp;
    private double distanceToQueryPoint;

    public GeoPointResult(Long id, double latitude, double longitude, Instant timestamp, double distanceToQueryPoint) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.distanceToQueryPoint = distanceToQueryPoint;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public double getDistanceToQueryPoint() {
        return distanceToQueryPoint;
    }
}
