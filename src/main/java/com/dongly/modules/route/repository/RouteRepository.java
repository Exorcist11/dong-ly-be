package com.dongly.modules.route.repository;

import com.dongly.modules.route.entity.CommonStatus;
import com.dongly.modules.route.entity.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RouteRepository extends JpaRepository<Route, UUID> {

    Optional<Route> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    @Query("""
        SELECT r FROM Route r
        JOIN FETCH r.originLocation orig
        JOIN FETCH r.destinationLocation dest
        WHERE (:originLocationId IS NULL OR orig.id = :originLocationId)
          AND (:destinationLocationId IS NULL OR dest.id = :destinationLocationId)
          AND (:status IS NULL OR r.status = :status)
          AND (
            :keyword IS NULL OR :keyword = ''
            OR LOWER(r.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
    """)
    Page<Route> searchRoutes(
            @Param("keyword") String keyword,
            @Param("originLocationId") UUID originLocationId,
            @Param("destinationLocationId") UUID destinationLocationId,
            @Param("status") CommonStatus status,
            Pageable pageable
    );

    @Query("""
        SELECT r FROM Route r
        JOIN FETCH r.originLocation orig
        JOIN FETCH r.destinationLocation dest
        LEFT JOIN FETCH r.stops s
        LEFT JOIN FETCH s.stopPoint sp
        LEFT JOIN FETCH sp.location spl
        WHERE r.id = :id
    """)
    Optional<Route> findByIdWithStops(@Param("id") UUID id);
}
