package com.dongly.modules.trip.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTripRequest {

    @NotNull(message = "Vui lòng chọn phương tiện")
    private UUID vehicleId;

    @NotNull(message = "Vui lòng chọn tài xế chính")
    private UUID driverId;

    @NotNull(message = "Vui lòng chọn phụ xe")
    private UUID assistantDriverId;

    @NotNull(message = "Thời điểm khởi hành không được để trống")
    private OffsetDateTime departureTime;

    private OffsetDateTime estimatedArrivalTime;

    @NotNull(message = "Giá vé cơ sở không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá vé cơ sở không được âm")
    private BigDecimal basePrice;

    private String note;
}
