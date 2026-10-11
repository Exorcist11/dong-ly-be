package com.dongly.modules.booking.repository;

import com.dongly.modules.booking.entity.BookingItem;
import com.dongly.modules.booking.entity.BookingItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingItemRepository extends JpaRepository<BookingItem, UUID> {

    List<BookingItem> findByBookingId(UUID bookingId);

    /**
     * Tìm danh sách BookingItem đang tích cực chiếm chỗ trên chuyến xe:
     * - Trạng thái CONFIRMED (đã bán).
     * - Hoặc trạng thái HELD (nhưng đơn booking cha chưa hết hạn tại thời điểm now).
     */
    @Query("""
        SELECT bi FROM BookingItem bi
        JOIN bi.booking b
        WHERE bi.trip.id = :tripId
          AND (
            bi.status = com.dongly.modules.booking.entity.BookingItemStatus.CONFIRMED
            OR (bi.status = com.dongly.modules.booking.entity.BookingItemStatus.HELD AND b.holdExpiresAt > :now)
          )
    """)
    List<BookingItem> findActiveItemsByTrip(@Param("tripId") UUID tripId, @Param("now") OffsetDateTime now);

    /**
     * Kiểm tra xem các ghế chỉ định có ghế nào đang bị chiếm chỗ (CONFIRMED hoặc HELD còn hạn) trên chuyến xe này không.
     */
    @Query("""
        SELECT bi FROM BookingItem bi
        JOIN bi.booking b
        WHERE bi.trip.id = :tripId
          AND bi.seat.id IN :seatIds
          AND (
            bi.status = com.dongly.modules.booking.entity.BookingItemStatus.CONFIRMED
            OR (bi.status = com.dongly.modules.booking.entity.BookingItemStatus.HELD AND b.holdExpiresAt > :now)
          )
    """)
    List<BookingItem> findConflictingSeats(
            @Param("tripId") UUID tripId,
            @Param("seatIds") Collection<UUID> seatIds,
            @Param("now") OffsetDateTime now
    );

    @Modifying
    @Query("""
        UPDATE BookingItem bi
        SET bi.status = com.dongly.modules.booking.entity.BookingItemStatus.EXPIRED,
            bi.updatedAt = :now
        WHERE bi.status = com.dongly.modules.booking.entity.BookingItemStatus.HELD
          AND bi.booking.id IN (
            SELECT b.id FROM Booking b
            WHERE b.status = com.dongly.modules.booking.entity.BookingStatus.EXPIRED
          )
    """)
    int expireOutdatedBookingItems(@Param("now") OffsetDateTime now);
}
