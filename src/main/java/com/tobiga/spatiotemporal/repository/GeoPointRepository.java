package com.tobiga.spatiotemporal.repository;

import com.tobiga.spatiotemporal.model.GeoPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface GeoPointRepository extends JpaRepository<GeoPoint, Long>, GeoPointRepositoryCustom {
    // Standard method to find points within a specific time range
    List<GeoPoint> findByTimestampBetween(Instant start, Instant end);
}
