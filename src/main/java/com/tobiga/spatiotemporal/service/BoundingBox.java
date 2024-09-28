package com.tobiga.spatiotemporal.service;

public class BoundingBox {
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private double minZ;
    private double maxZ;


    public static BoundingBox combine(BoundingBox box1, BoundingBox box2) {
        double combinedMinX = Math.min(box1.getMinX(), box2.getMinX());
        double combinedMaxX = Math.max(box1.getMaxX(), box2.getMaxX());

        double combinedMinY = Math.min(box1.getMinY(), box2.getMinY());
        double combinedMaxY = Math.max(box1.getMaxY(), box2.getMaxY());

        double combinedMinZ = Math.min(box1.getMinZ(), box2.getMinZ());
        double combinedMaxZ = Math.max(box1.getMaxZ(), box2.getMaxZ());

        // Create and return the new bounding box that contains both
        return new BoundingBox(combinedMinX, combinedMaxX, combinedMinY, combinedMaxY, combinedMinZ, combinedMaxZ);
    }

    public BoundingBox(double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    public BoundingBox(DataPoint point) {
        this.minX = point.getX();
        this.maxX = point.getX();
        this.minY = point.getY();
        this.maxY = point.getY();
        this.minZ = point.getZ();
        this.maxZ = point.getZ();
    }


    // Getters and Setters
    public double getMinX() {
        return minX;
    }

    public void setMinX(double minX) {
        this.minX = minX;
    }

    public double getMaxX() {
        return maxX;
    }

    public void setMaxX(double maxX) {
        this.maxX = maxX;
    }

    public double getMinY() {
        return minY;
    }

    public void setMinY(double minY) {
        this.minY = minY;
    }

    public double getMaxY() {
        return maxY;
    }

    public void setMaxY(double maxY) {
        this.maxY = maxY;
    }

    public double getMinZ() {
        return minZ;
    }

    public void setMinZ(double minZ) {
        this.minZ = minZ;
    }

    public double getMaxZ() {
        return maxZ;
    }

    public void setMaxZ(double maxZ) {
        this.maxZ = maxZ;
    }

    // Check if this bounding box contains a point
    public boolean contains(DataPoint point) {
        return (point.getX() >= minX && point.getX() <= maxX) &&
                (point.getY() >= minY && point.getY() <= maxY) &&
                (point.getZ() >= minZ && point.getZ() <= maxZ);
    }

    // Check if this bounding box intersects another bounding box
    public boolean intersects(BoundingBox other) {
        return (this.minX <= other.maxX && this.maxX >= other.minX) &&
                (this.minY <= other.maxY && this.maxY >= other.minY) &&
                (this.minZ <= other.maxZ && this.maxZ >= other.minZ);
    }

    public double distanceTo(Coordinate queryPoint) {
        double dx = Math.max(0, Math.max(minX - queryPoint.getX(), queryPoint.getX() - maxX));
        double dy = Math.max(0, Math.max(minY - queryPoint.getY(), queryPoint.getY() - maxY));
        double dz = Math.max(0, Math.max(minZ - queryPoint.getZ(), queryPoint.getZ() - maxZ));

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double enlargementNeeded(DataPoint point) {
        double enlargementX = 0, enlargementY = 0, enlargementZ = 0;

        // Check if the point extends the bounding box in the X axis
        if (point.getX() < minX) {
            enlargementX = minX - point.getX();
        } else if (point.getX() > maxX) {
            enlargementX = point.getX() - maxX;
        }

        // Check if the point extends the bounding box in the Y axis
        if (point.getY() < minY) {
            enlargementY = minY - point.getY();
        } else if (point.getY() > maxY) {
            enlargementY = point.getY() - maxY;
        }

        // Check if the point extends the bounding box in the Z axis
        if (point.getZ() < minZ) {
            enlargementZ = minZ - point.getZ();
        } else if (point.getZ() > maxZ) {
            enlargementZ = point.getZ() - maxZ;
        }

        // Sum up the enlargement in all dimensions
        return enlargementX + enlargementY + enlargementZ;
    }


    @Override
    public String toString() {
        return "BoundingBox{" +
                "minX=" + minX +
                ", maxX=" + maxX +
                ", minY=" + minY +
                ", maxY=" + maxY +
                ", minZ=" + minZ +
                ", maxZ=" + maxZ +
                '}';
    }
}
