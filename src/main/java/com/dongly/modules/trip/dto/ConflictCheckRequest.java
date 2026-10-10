package com.dongly.modules.trip.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictCheckRequest {

    private UUID tripId; // Chuyến hiện tại đang chỉnh sửa (nếu có, để loại trừ)

    private UUID vehicleId;

    private UUID driverId;

    private UUID assistantDriverId;

    @NotNull(message = "Thời điểm khởi hành không được để trống")
    private OffsetDateTime departureTime;

    @NotNull(message = "Thời điểm đến dự kiến không được để trống")
    private OffsetDateTime estimatedArrivalTime;
}
