package com.tobiga.spatiotemporal.service;

public class DataPoint {
    private double x;
    private double y;
    private double z;
    private String externalId;
    private long timestamp;

    public DataPoint(double x, double y, double z, String externalId, long timestamp) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.externalId = externalId;
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

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Method to calculate distance from this point to another
    public double distanceTo(DataPoint other) {
        return Math.sqrt(
                Math.pow(this.x - other.x, 2) +
                        Math.pow(this.y - other.y, 2) +
                        Math.pow(this.z - other.z, 2)
        );
    }

    @Override
    public String toString() {
        return "DataPoint{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", externalId='" + externalId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
