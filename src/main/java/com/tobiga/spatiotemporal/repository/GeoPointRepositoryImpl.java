package com.tobiga.spatiotemporal.repository;

import com.tobiga.spatiotemporal.model.GeoPoint;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import java.time.Instant;

import java.util.List;

@Repository
public class GeoPointRepositoryImpl implements GeoPointRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<GeoPoint> findGeoPointsNearQueryPoint(List<Double> anchorDistances, double tolerance, Instant start, Instant end) {
        String anchorDistancesArray = anchorDistances.toString().replace("[", "{").replace("]", "}");

        String sql = """
                WITH anchor_distances_query AS (
                    SELECT unnest(CAST(?1 AS double precision[])) AS anchor_distance, generate_series(1, 10) AS anchor_id
                )
                SELECT gp.id, gp.x, gp.y, gp.z, gp.timestamp, SUM(ABS(gd.anchor_distance - adq.anchor_distance)) AS distance_diff
                FROM geo_points gp
                JOIN geo_point_distances gd ON gp.id = gd.geo_point_id
                JOIN anchor_distances_query adq ON gd.anchor_id = adq.anchor_id
                WHERE gp.timestamp BETWEEN ?3 AND ?4  -- Add time filtering here
                GROUP BY gp.id, gp.x, gp.y, gp.z, gp.timestamp
                HAVING SUM(ABS(gd.anchor_distance - adq.anchor_distance)) < ?2
                ORDER BY distance_diff;
                """;

        Query query = entityManager.createNativeQuery(sql, GeoPoint.class);

        query.setParameter(1, anchorDistancesArray);
        query.setParameter(2, tolerance);
        query.setParameter(3, start);
        query.setParameter(4, end);

        return query.getResultList();
    }

}
