package com.dongly.modules.fleet.dto;

import com.dongly.modules.fleet.entity.VehicleStatus;
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
public class UpdateVehicleStatusRequest {

    @Schema(description = "Trạng thái mới của phương tiện", example = "MAINTENANCE")
    @NotNull(message = "Trạng thái không được để trống")
    private VehicleStatus status;
}
