package com.tobiga.spatiotemporal.dto;

import java.time.Instant;

public class GeoPointRequest {
    private double x;
    private double y;
    private double z;
    private Instant timestamp;

    // Constructors
    public GeoPointRequest() {
    }

    public GeoPointRequest(double x, double y, double z, Instant timestamp) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
