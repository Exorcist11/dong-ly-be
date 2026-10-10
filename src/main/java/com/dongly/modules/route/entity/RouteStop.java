package com.dongly.modules.route.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Thực thể Điểm dừng trên Tuyến (RouteStop).
 */
@Entity
@Table(name = "route_stops")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_point_id", nullable = false)
    private StopPoint stopPoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 20, nullable = false)
    @Builder.Default
    private RouteDirectionType direction = RouteDirectionType.OUTBOUND;

    @Column(name = "sequence", nullable = false)
    private Integer sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "stop_type", length = 20, nullable = false)
    @Builder.Default
    private RouteStopType stopType = RouteStopType.BOTH;

    @Column(name = "extra_price", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal extraPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private CommonStatus status = CommonStatus.ACTIVE;

    @CreatedBy
    @Column(name = "created_by", length = 50)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteStop routeStop = (RouteStop) o;
        return Objects.equals(id, routeStop.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
