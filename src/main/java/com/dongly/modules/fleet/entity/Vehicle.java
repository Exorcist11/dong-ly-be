package com.dongly.modules.fleet.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Thực thể Vehicle: Quản lý phương tiện vận tải của nhà xe Đông Lý
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "plate_number", nullable = false, unique = true, length = 20)
    private String plateNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 30)
    private VehicleType vehicleType;

    @Column(name = "brand", nullable = false, length = 100)
    private String brand;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "manufacture_year")
    private Integer manufactureYear;

    @Column(name = "total_floors", nullable = false)
    @Builder.Default
    private Integer totalFloors = 1;

    @Column(name = "total_rows", nullable = false)
    @Builder.Default
    private Integer totalRows = 5;

    @Column(name = "total_columns", nullable = false)
    @Builder.Default
    private Integer totalColumns = 4;

    @Column(name = "total_seats", nullable = false)
    @Builder.Default
    private Integer totalSeats = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private VehicleStatus status = VehicleStatus.ACTIVE;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VehicleSeat> seats = new ArrayList<>();

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
        if (status == null) {
            status = VehicleStatus.ACTIVE;
        }
        if (totalFloors == null) {
            totalFloors = 1;
        }
        if (totalRows == null) {
            totalRows = 5;
        }
        if (totalColumns == null) {
            totalColumns = 4;
        }
        if (totalSeats == null) {
            totalSeats = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
