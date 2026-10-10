package com.dongly.modules.route.repository;

import com.dongly.modules.route.entity.RouteStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RouteStopRepository extends JpaRepository<RouteStop, UUID> {

    List<RouteStop> findByRouteIdOrderBySequenceAsc(UUID routeId);

    long countByStopPointId(UUID stopPointId);

    boolean existsByStopPointId(UUID stopPointId);

    @Modifying
    @Query("DELETE FROM RouteStop rs WHERE rs.route.id = :routeId")
    void deleteAllByRouteId(@Param("routeId") UUID routeId);
}
