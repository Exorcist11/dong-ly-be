package com.dongly.modules.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin hành khách và điểm đón/trả cho từng ghế")
public class PassengerItemRequest {

    @NotNull(message = "ID ghế không được để trống")
    @Schema(description = "ID ghế", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID seatId;

    @NotBlank(message = "Tên hành khách không được để trống")
    @Schema(description = "Họ tên hành khách ngồi ghế này", example = "Nguyễn Văn Nam", requiredMode = Schema.RequiredMode.REQUIRED)
    private String passengerName;

    @Schema(description = "Số điện thoại hành khách", example = "0912345678")
    private String passengerPhone;

    @Schema(description = "ID điểm đón (tùy chọn)")
    private UUID pickupStopId;

    @Schema(description = "ID điểm trả (tùy chọn)")
    private UUID dropoffStopId;
}
