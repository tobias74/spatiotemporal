package com.tobiga.spatiotemporal.model;

public class AnchorPoint {

    private int anchorId;
    private double[] coordinates;

    public AnchorPoint(int anchorId, double[] coordinates) {
        this.anchorId = anchorId;
        this.coordinates = coordinates;
    }

    public int getAnchorId() {
        return anchorId;
    }

    public void setAnchorId(int anchorId) {
        this.anchorId = anchorId;
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(double[] coordinates) {
        this.coordinates = coordinates;
    }
}
