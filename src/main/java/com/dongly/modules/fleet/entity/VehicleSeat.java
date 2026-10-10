package com.dongly.modules.fleet.entity;

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
 * Thực thể VehicleSeat: Từng ghế/giường vật lý trên sơ đồ xe
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vehicle_seats")
public class VehicleSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "seat_code", nullable = false, length = 20)
    private String seatCode;

    @Column(name = "floor", nullable = false)
    @Builder.Default
    private Integer floor = 1;

    @Column(name = "row_index", nullable = false)
    private Integer rowIndex;

    @Column(name = "column_index", nullable = false)
    private Integer columnIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 30)
    @Builder.Default
    private SeatType seatType = SeatType.STANDARD;

    @Column(name = "extra_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal extraPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SeatStatus status = SeatStatus.ACTIVE;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

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
        if (floor == null) {
            floor = 1;
        }
        if (seatType == null) {
            seatType = SeatType.STANDARD;
        }
        if (extraPrice == null) {
            extraPrice = BigDecimal.ZERO;
        }
        if (status == null) {
            status = SeatStatus.ACTIVE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
