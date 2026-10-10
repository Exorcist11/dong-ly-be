package com.dongly.modules.fleet.dto;

import com.dongly.modules.fleet.entity.Vehicle;
import com.dongly.modules.fleet.entity.VehicleSeat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSeatLayoutResponse {
    private UUID vehicleId;
    private String plateNumber;
    private Integer totalFloors;
    private Integer totalRows;
    private Integer totalColumns;
    private Integer totalSeats;
    private List<VehicleSeatResponse> seats;

    public static VehicleSeatLayoutResponse of(Vehicle vehicle, List<VehicleSeat> seats) {
        return VehicleSeatLayoutResponse.builder()
                .vehicleId(vehicle.getId())
                .plateNumber(vehicle.getPlateNumber())
                .totalFloors(vehicle.getTotalFloors())
                .totalRows(vehicle.getTotalRows())
                .totalColumns(vehicle.getTotalColumns())
                .totalSeats(vehicle.getTotalSeats())
                .seats(seats.stream().map(VehicleSeatResponse::fromEntity).toList())
                .build();
    }
}
