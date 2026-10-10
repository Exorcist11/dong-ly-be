package com.dongly.modules.fleet.dto;

import com.dongly.modules.fleet.entity.Vehicle;
import com.dongly.modules.fleet.entity.VehicleStatus;
import com.dongly.modules.fleet.entity.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSummaryResponse {
    private UUID id;
    private String plateNumber;
    private VehicleType vehicleType;
    private String brand;
    private String model;
    private Integer manufactureYear;
    private Integer totalFloors;
    private Integer totalRows;
    private Integer totalColumns;
    private Integer totalSeats;
    private VehicleStatus status;
    private String description;
    private String createdBy;
    private String updatedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static VehicleSummaryResponse fromEntity(Vehicle vehicle) {
        return VehicleSummaryResponse.builder()
                .id(vehicle.getId())
                .plateNumber(vehicle.getPlateNumber())
                .vehicleType(vehicle.getVehicleType())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .manufactureYear(vehicle.getManufactureYear())
                .totalFloors(vehicle.getTotalFloors())
                .totalRows(vehicle.getTotalRows())
                .totalColumns(vehicle.getTotalColumns())
                .totalSeats(vehicle.getTotalSeats())
                .status(vehicle.getStatus())
                .description(vehicle.getDescription())
                .createdBy(vehicle.getCreatedBy())
                .updatedBy(vehicle.getUpdatedBy())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }
}
