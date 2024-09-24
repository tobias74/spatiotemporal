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
    public List<GeoPoint> findGeoPointsNearQueryPoint(List<Double> anchorDistances, Instant start, Instant end, int limit, int offset) {
        String anchorDistancesArray = anchorDistances.toString().replace("[", "{").replace("]", "}");

        String sql = """
                WITH anchor_distances_query AS (
                    SELECT unnest(CAST(?1 AS double precision[])) AS anchor_distance, generate_series(1, 10) AS anchor_id
                )
                SELECT gp.id, gp.x, gp.y, gp.z, gp.timestamp,
                       SUM(ABS(gd.anchor_distance - adq.anchor_distance)) AS distance_diff
                FROM geo_points gp
                JOIN geo_point_distances gd ON gp.id = gd.geo_point_id
                JOIN anchor_distances_query adq ON gd.anchor_id = adq.anchor_id
                WHERE gp.timestamp BETWEEN ?2 AND ?3
                GROUP BY gp.id, gp.x, gp.y, gp.z, gp.timestamp
                ORDER BY distance_diff
                LIMIT ?4 OFFSET ?5;
                """;

        Query query = entityManager.createNativeQuery(sql, GeoPoint.class);
        query.setParameter(1, anchorDistancesArray);
        query.setParameter(2, start);
        query.setParameter(3, end);
        query.setParameter(4, limit);
        query.setParameter(5, offset);

        return query.getResultList();
    }

}
