package com.dongly.modules.booking.repository;

import com.dongly.modules.booking.entity.Booking;
import com.dongly.modules.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByBookingCode(String bookingCode);

    boolean existsByBookingCode(String bookingCode);

    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.trip t
        LEFT JOIN FETCH t.route r
        LEFT JOIN FETCH t.vehicle v
        LEFT JOIN FETCH b.customer c
        LEFT JOIN FETCH b.items i
        WHERE b.id = :id
    """)
    Optional<Booking> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.status = com.dongly.modules.booking.entity.BookingStatus.HELD
          AND b.holdExpiresAt < :now
    """)
    List<Booking> findExpiredHeldBookings(@Param("now") OffsetDateTime now);

    @Modifying
    @Query("""
        UPDATE Booking b
        SET b.status = com.dongly.modules.booking.entity.BookingStatus.EXPIRED,
            b.updatedAt = :now
        WHERE b.status = com.dongly.modules.booking.entity.BookingStatus.HELD
          AND b.holdExpiresAt < :now
    """)
    int expireOutdatedBookings(@Param("now") OffsetDateTime now);

    @Query(value = """
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.trip t
        LEFT JOIN FETCH t.route r
        LEFT JOIN FETCH t.vehicle v
        LEFT JOIN FETCH b.customer c
        WHERE (:tripId IS NULL OR t.id = :tripId)
          AND (:status IS NULL OR b.status = :status)
          AND (CAST(:fromDate AS java.time.OffsetDateTime) IS NULL OR t.departureTime >= :fromDate)
          AND (CAST(:toDate AS java.time.OffsetDateTime) IS NULL OR t.departureTime <= :toDate)
          AND (
            :keyword IS NULL OR :keyword = ''
            OR LOWER(b.bookingCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(v.plateNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(t.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
    """, countQuery = """
        SELECT COUNT(b) FROM Booking b
        LEFT JOIN b.trip t
        LEFT JOIN t.vehicle v
        LEFT JOIN b.customer c
        WHERE (:tripId IS NULL OR t.id = :tripId)
          AND (:status IS NULL OR b.status = :status)
          AND (CAST(:fromDate AS java.time.OffsetDateTime) IS NULL OR t.departureTime >= :fromDate)
          AND (CAST(:toDate AS java.time.OffsetDateTime) IS NULL OR t.departureTime <= :toDate)
          AND (
            :keyword IS NULL OR :keyword = ''
            OR LOWER(b.bookingCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(v.plateNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(t.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
    """)
    org.springframework.data.domain.Page<Booking> searchBookings(
            @Param("keyword") String keyword,
            @Param("status") BookingStatus status,
            @Param("tripId") UUID tripId,
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate,
            org.springframework.data.domain.Pageable pageable
    );
}
