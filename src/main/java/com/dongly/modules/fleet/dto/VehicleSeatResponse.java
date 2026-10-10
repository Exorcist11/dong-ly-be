package com.dongly.modules.fleet.dto;

import com.dongly.modules.fleet.entity.SeatStatus;
import com.dongly.modules.fleet.entity.SeatType;
import com.dongly.modules.fleet.entity.VehicleSeat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSeatResponse {
    private UUID id;
    private UUID vehicleId;
    private String seatCode;
    private Integer floor;
    private Integer rowIndex;
    private Integer columnIndex;
    private SeatType seatType;
    private BigDecimal extraPrice;
    private SeatStatus status;
    private String createdBy;
    private String updatedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static VehicleSeatResponse fromEntity(VehicleSeat seat) {
        return VehicleSeatResponse.builder()
                .id(seat.getId())
                .vehicleId(seat.getVehicle().getId())
                .seatCode(seat.getSeatCode())
                .floor(seat.getFloor())
                .rowIndex(seat.getRowIndex())
                .columnIndex(seat.getColumnIndex())
                .seatType(seat.getSeatType())
                .extraPrice(seat.getExtraPrice())
                .status(seat.getStatus())
                .createdBy(seat.getCreatedBy())
                .updatedBy(seat.getUpdatedBy())
                .createdAt(seat.getCreatedAt())
                .updatedAt(seat.getUpdatedAt())
                .build();
    }
}
