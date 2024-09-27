package com.tobiga.spatiotemporal.dto;

public class GeopointQuery {
    private double lat;
    private double lon;
    private double alt;
    private long startTime;
    private long endTime;
    private int offset = 0;   // Default offset
    private int limit = 10;   // Default limit

    // Getters and setters for each field
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLon() { return lon; }
    public void setLon(double lon) { this.lon = lon; }

    public double getAlt() { return alt; }
    public void setAlt(double alt) { this.alt = alt; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
