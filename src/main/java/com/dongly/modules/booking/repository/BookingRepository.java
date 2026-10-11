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
}
