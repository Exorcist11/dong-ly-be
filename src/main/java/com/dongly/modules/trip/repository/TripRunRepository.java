package com.dongly.modules.trip.repository;

import com.dongly.modules.trip.entity.TripRun;
import com.dongly.modules.trip.entity.TripRunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TripRunRepository extends JpaRepository<TripRun, UUID> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    List<TripRun> findByStatus(TripRunStatus status);

    @Query("""
        SELECT tr FROM TripRun tr
        LEFT JOIN FETCH tr.route r
        LEFT JOIN FETCH tr.defaultVehicle v
        LEFT JOIN FETCH tr.defaultDriver d
        LEFT JOIN FETCH tr.defaultAssistantDriver ad
        WHERE (:status IS NULL OR tr.status = :status)
          AND (:routeId IS NULL OR tr.route.id = :routeId)
          AND (
            :keyword IS NULL OR :keyword = ''
            OR LOWER(tr.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(tr.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
    """)
    Page<TripRun> searchTripRuns(
            @Param("keyword") String keyword,
            @Param("routeId") UUID routeId,
            @Param("status") TripRunStatus status,
            Pageable pageable
    );
}
