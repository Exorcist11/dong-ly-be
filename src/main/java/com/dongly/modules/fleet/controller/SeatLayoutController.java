package com.dongly.modules.fleet.controller;

import com.dongly.common.api.ApiResponse;
import com.dongly.modules.fleet.dto.ConfigureSeatLayoutRequest;
import com.dongly.modules.fleet.dto.UpdateSeatStatusRequest;
import com.dongly.modules.fleet.dto.VehicleSeatLayoutResponse;
import com.dongly.modules.fleet.dto.VehicleSeatResponse;
import com.dongly.modules.fleet.service.SeatLayoutService;
import com.dongly.security.CurrentUser;
import com.dongly.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Seat Layout Management", description = "Các endpoint quản trị sơ đồ ghế và vị trí ngồi trên phương tiện")
@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/seats")
public class SeatLayoutController {

    private final SeatLayoutService seatLayoutService;

    public SeatLayoutController(SeatLayoutService seatLayoutService) {
        this.seatLayoutService = seatLayoutService;
    }

    @Operation(summary = "Lấy sơ đồ ghế chi tiết của phương tiện (Yêu cầu quyền FLEET_READ)")
    @GetMapping
    @PreAuthorize("hasAuthority('FLEET_READ')")
    public ResponseEntity<ApiResponse<VehicleSeatLayoutResponse>> getSeatLayout(
            @PathVariable UUID vehicleId
    ) {
        VehicleSeatLayoutResponse response = seatLayoutService.getSeatLayout(vehicleId);
        return ResponseEntity.ok(ApiResponse.success("Lấy sơ đồ ghế thành công", response));
    }

    @Operation(summary = "Cấu hình / Thay thế toàn bộ sơ đồ ghế của phương tiện (Yêu cầu quyền FLEET_MANAGE)")
    @PutMapping
    @PreAuthorize("hasAuthority('FLEET_MANAGE')")
    public ResponseEntity<ApiResponse<VehicleSeatLayoutResponse>> configureSeatLayout(
            @PathVariable UUID vehicleId,
            @Valid @RequestBody ConfigureSeatLayoutRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        VehicleSeatLayoutResponse response = seatLayoutService.configureSeatLayout(vehicleId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cấu hình sơ đồ ghế thành công", response));
    }

    @Operation(summary = "Cập nhật trạng thái một ghế cụ thể (Yêu cầu quyền FLEET_MANAGE)")
    @PatchMapping("/{seatId}/status")
    @PreAuthorize("hasAuthority('FLEET_MANAGE')")
    public ResponseEntity<ApiResponse<VehicleSeatResponse>> updateSeatStatus(
            @PathVariable UUID vehicleId,
            @PathVariable UUID seatId,
            @Valid @RequestBody UpdateSeatStatusRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        VehicleSeatResponse response = seatLayoutService.updateSeatStatus(vehicleId, seatId, request.getStatus(), currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái ghế thành công", response));
    }
}
