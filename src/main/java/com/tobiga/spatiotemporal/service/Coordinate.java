package com.tobiga.spatiotemporal.service;

public interface Coordinate {
    double getX();
    double getY();
    double getZ();

    default double distanceTo(Coordinate other) {
        return Math.sqrt(
                Math.pow(this.getX() - other.getX(), 2) +
                        Math.pow(this.getY() - other.getY(), 2) +
                        Math.pow(this.getZ() - other.getZ(), 2)
        );
    }
}
