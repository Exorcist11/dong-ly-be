package com.dongly.modules.fleet.dto;

import com.dongly.modules.fleet.entity.DriverStatus;
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
public class UpdateDriverStatusRequest {

    @Schema(description = "Trạng thái mới của tài xế", example = "ON_LEAVE")
    @NotNull(message = "Trạng thái tài xế không được để trống")
    private DriverStatus status;
}
