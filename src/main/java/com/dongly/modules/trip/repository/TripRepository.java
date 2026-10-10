package com.dongly.modules.trip.repository;

import com.dongly.modules.trip.entity.Trip;
import com.dongly.modules.trip.entity.TripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    boolean existsByTripRunIdAndDepartureTime(UUID tripRunId, OffsetDateTime departureTime);

    long countByTripRunId(UUID tripRunId);

    List<Trip> findByTripRunIdAndDepartureTimeBetween(
            UUID tripRunId,
            OffsetDateTime fromDate,
            OffsetDateTime toDate
    );

    @Query("""
        SELECT t FROM Trip t
        LEFT JOIN FETCH t.route r
        LEFT JOIN FETCH t.vehicle v
        LEFT JOIN FETCH t.driver d
        LEFT JOIN FETCH t.assistantDriver ad
        LEFT JOIN FETCH t.tripRun tr
        WHERE t.id = :id
    """)
    Optional<Trip> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
        SELECT t FROM Trip t
        LEFT JOIN FETCH t.route r
        LEFT JOIN FETCH t.vehicle v
        LEFT JOIN FETCH t.driver d
        LEFT JOIN FETCH t.assistantDriver ad
        WHERE (:routeId IS NULL OR t.route.id = :routeId)
          AND (:vehicleId IS NULL OR t.vehicle.id = :vehicleId)
          AND (:driverId IS NULL OR t.driver.id = :driverId OR t.assistantDriver.id = :driverId)
          AND (:status IS NULL OR t.status = :status)
          AND (CAST(:fromDate AS java.time.OffsetDateTime) IS NULL OR t.departureTime >= :fromDate)
          AND (CAST(:toDate AS java.time.OffsetDateTime) IS NULL OR t.departureTime <= :toDate)
          AND (
            :keyword IS NULL OR :keyword = ''
            OR LOWER(t.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(v.plateNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(d.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(ad.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
    """)
    Page<Trip> searchTrips(
            @Param("keyword") String keyword,
            @Param("routeId") UUID routeId,
            @Param("vehicleId") UUID vehicleId,
            @Param("driverId") UUID driverId,
            @Param("status") TripStatus status,
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate,
            Pageable pageable
    );

    @Query("""
        SELECT t FROM Trip t
        JOIN FETCH t.vehicle v
        WHERE t.vehicle.id = :vehicleId
          AND t.status <> com.dongly.modules.trip.entity.TripStatus.CANCELLED
          AND (:excludeTripId IS NULL OR t.id <> :excludeTripId)
          AND t.departureTime < :windowEnd
          AND t.estimatedArrivalTime > :windowStart
    """)
    List<Trip> findActiveTripsByVehicleInWindow(
            @Param("vehicleId") UUID vehicleId,
            @Param("windowStart") OffsetDateTime windowStart,
            @Param("windowEnd") OffsetDateTime windowEnd,
            @Param("excludeTripId") UUID excludeTripId
    );

    @Query("""
        SELECT t FROM Trip t
        JOIN FETCH t.driver d
        JOIN FETCH t.assistantDriver ad
        WHERE (t.driver.id = :driverId OR t.assistantDriver.id = :driverId)
          AND t.status <> com.dongly.modules.trip.entity.TripStatus.CANCELLED
          AND (:excludeTripId IS NULL OR t.id <> :excludeTripId)
          AND t.departureTime < :windowEnd
          AND t.estimatedArrivalTime > :windowStart
    """)
    List<Trip> findActiveTripsByDriverInWindow(
            @Param("driverId") UUID driverId,
            @Param("windowStart") OffsetDateTime windowStart,
            @Param("windowEnd") OffsetDateTime windowEnd,
            @Param("excludeTripId") UUID excludeTripId
    );
}
