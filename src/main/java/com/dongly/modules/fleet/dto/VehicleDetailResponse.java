package com.dongly.modules.fleet.dto;

import com.dongly.modules.fleet.entity.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDetailResponse {
    private VehicleSummaryResponse vehicle;
    private List<VehicleSeatResponse> seats;

    public static VehicleDetailResponse of(Vehicle vehicle, List<VehicleSeatResponse> seats) {
        return VehicleDetailResponse.builder()
                .vehicle(VehicleSummaryResponse.fromEntity(vehicle))
                .seats(seats)
                .build();
    }
}
