package com.dongly.modules.booking.entity;

import com.dongly.modules.fleet.entity.VehicleSeat;
import com.dongly.modules.route.entity.RouteStop;
import com.dongly.modules.trip.entity.Trip;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Thực thể BookingItem: Bản ghi chi tiết từng ghế và hành khách trong đơn đặt vé
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "booking_items")
public class BookingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private VehicleSeat seat;

    @Column(name = "seat_code", nullable = false, length = 20)
    private String seatCode;

    @Column(name = "passenger_name", length = 100)
    private String passengerName;

    @Column(name = "passenger_phone", length = 20)
    private String passengerPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_stop_id")
    private RouteStop pickupStop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dropoff_stop_id")
    private RouteStop dropoffStop;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal basePrice = BigDecimal.ZERO;

    @Column(name = "seat_extra_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal seatExtraPrice = BigDecimal.ZERO;

    @Column(name = "pickup_extra_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal pickupExtraPrice = BigDecimal.ZERO;

    @Column(name = "dropoff_extra_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal dropoffExtraPrice = BigDecimal.ZERO;

    @Column(name = "final_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal finalPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BookingItemStatus status = BookingItemStatus.HELD;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = BookingItemStatus.HELD;
        }
        if (basePrice == null) basePrice = BigDecimal.ZERO;
        if (seatExtraPrice == null) seatExtraPrice = BigDecimal.ZERO;
        if (pickupExtraPrice == null) pickupExtraPrice = BigDecimal.ZERO;
        if (dropoffExtraPrice == null) dropoffExtraPrice = BigDecimal.ZERO;
        if (finalPrice == null) finalPrice = basePrice.add(seatExtraPrice).add(pickupExtraPrice).add(dropoffExtraPrice);
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
