package com.tobiga.spatiotemporal.repository;

import com.tobiga.spatiotemporal.model.GeoPoint;
import java.util.List;

public interface GeoPointRepositoryCustom {
    // Custom method for finding geo points near a query point
    List<GeoPoint> findGeoPointsNearQueryPoint(List<Double> anchorDistances, double tolerance);
}
