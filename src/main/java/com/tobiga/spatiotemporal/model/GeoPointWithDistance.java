package com.tobiga.spatiotemporal.model;

public class GeoPointWithDistance {

    private GeoPoint geoPoint;
    private double distanceToQueryPoint;

    public GeoPointWithDistance(GeoPoint geoPoint, double distanceToQueryPoint) {
        this.geoPoint = geoPoint;
        this.distanceToQueryPoint = distanceToQueryPoint;
    }

    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public double getDistanceToQueryPoint() {
        return distanceToQueryPoint;
    }
}
