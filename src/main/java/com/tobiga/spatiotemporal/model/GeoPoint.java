package com.tobiga.spatiotemporal.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

@Entity @Table(name = "geo_points")
public class GeoPoint {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double x;
    private double y;
    private double z;

    @Column(nullable = false)
    private Instant timestamp;

    // Store anchor distances as an ElementCollection with embedded AnchorDistance
    @ElementCollection @CollectionTable(name = "geo_point_distances", joinColumns = @JoinColumn(name = "geo_point_id"))
    private List<AnchorDistance> anchorDistances;

    // Constructors
    public GeoPoint() {
    }

    public GeoPoint(double x, double y, double z, Instant timestamp, List<AnchorDistance> anchorDistances) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
        this.anchorDistances = anchorDistances;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public List<AnchorDistance> getAnchorDistances() {
        return anchorDistances;
    }

    public void setAnchorDistances(List<AnchorDistance> anchorDistances) {
        this.anchorDistances = anchorDistances;
    }
}
