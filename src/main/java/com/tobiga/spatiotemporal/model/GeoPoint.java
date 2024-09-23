package com.tobiga.spatiotemporal.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "geo_points")
public class GeoPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double x;
    private double y;
    private double z;

    @Column(nullable = false)
    private Instant timestamp;

    @ElementCollection
    @Column(name = "distances", nullable = false)
    private List<Double> anchorDistances;

    // Constructors, getters, setters

    public GeoPoint() {
    }

    public GeoPoint(double x, double y, double z, Instant timestamp, List<Double> anchorDistances) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
        this.anchorDistances = anchorDistances;
    }

    // Getters and setters...
}
