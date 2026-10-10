package com.dongly.modules.fleet.dto;

import com.dongly.modules.fleet.entity.SeatStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSeatStatusRequest {

    @Schema(description = "Trạng thái mới của ghế", example = "BLOCKED")
    @NotNull(message = "Trạng thái ghế không được để trống")
    private SeatStatus status;
}
