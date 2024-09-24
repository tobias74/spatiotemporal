package com.tobiga.spatiotemporal.model;

import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class AnchorDistance implements Serializable {

    private int anchorId; // The anchor point ID
    private double anchorDistance; // The distance to the anchor point

    // Constructors
    public AnchorDistance() {
    }

    public AnchorDistance(int anchorId, double anchorDistance) {
        this.anchorId = anchorId;
        this.anchorDistance = anchorDistance;
    }

    // Getters and setters
    public int getAnchorId() {
        return anchorId;
    }

    public void setAnchorId(int anchorId) {
        this.anchorId = anchorId;
    }

    public double getAnchorDistance() {
        return anchorDistance;
    }

    public void setAnchorDistance(double anchorDistance) {
        this.anchorDistance = anchorDistance;
    }
}
