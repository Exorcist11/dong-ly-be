package com.dongly.modules.fleet.controller;

import com.dongly.common.api.ApiResponse;
import com.dongly.common.api.PageResponse;
import com.dongly.modules.fleet.dto.CreateVehicleRequest;
import com.dongly.modules.fleet.dto.UpdateVehicleRequest;
import com.dongly.modules.fleet.dto.UpdateVehicleStatusRequest;
import com.dongly.modules.fleet.dto.VehicleDetailResponse;
import com.dongly.modules.fleet.dto.VehicleSummaryResponse;
import com.dongly.modules.fleet.entity.VehicleStatus;
import com.dongly.modules.fleet.entity.VehicleType;
import com.dongly.modules.fleet.service.VehicleService;
import com.dongly.security.CurrentUser;
import com.dongly.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Vehicle Management", description = "Các endpoint quản trị phương tiện vận tải và đội xe")
@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @Operation(summary = "Lấy danh sách phương tiện có phân trang, lọc và tìm kiếm (Yêu cầu quyền FLEET_READ)")
    @GetMapping
    @PreAuthorize("hasAuthority('FLEET_READ')")
    public ResponseEntity<PageResponse<VehicleSummaryResponse>> searchVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) VehicleType vehicleType,
            @RequestParam(required = false) VehicleStatus status
    ) {
        if (size > 100) {
            size = 100;
        }

        Sort sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sort != null && sort.contains(",")) {
            String[] parts = sort.split(",");
            Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            sortObj = Sort.by(direction, parts[0]);
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), size, sortObj);
        Page<VehicleSummaryResponse> pageResult = vehicleService.searchVehicles(keyword, vehicleType, status, pageable);

        return ResponseEntity.ok(PageResponse.of(pageResult, "Lấy danh sách phương tiện thành công"));
    }

    @Operation(summary = "Lấy chi tiết phương tiện bao gồm danh sách ghế (Yêu cầu quyền FLEET_READ)")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FLEET_READ')")
    public ResponseEntity<ApiResponse<VehicleDetailResponse>> getVehicleById(@PathVariable UUID id) {
        VehicleDetailResponse response = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin phương tiện thành công", response));
    }

    @Operation(summary = "Tạo mới phương tiện (Yêu cầu quyền FLEET_MANAGE)")
    @PostMapping
    @PreAuthorize("hasAuthority('FLEET_MANAGE')")
    public ResponseEntity<ApiResponse<VehicleDetailResponse>> createVehicle(
            @Valid @RequestBody CreateVehicleRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        VehicleDetailResponse response = vehicleService.createVehicle(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo phương tiện thành công", response));
    }

    @Operation(summary = "Cập nhật thông tin phương tiện (Yêu cầu quyền FLEET_MANAGE)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FLEET_MANAGE')")
    public ResponseEntity<ApiResponse<VehicleDetailResponse>> updateVehicle(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVehicleRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        VehicleDetailResponse response = vehicleService.updateVehicle(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phương tiện thành công", response));
    }

    @Operation(summary = "Cập nhật trạng thái hoạt động của phương tiện (Yêu cầu quyền FLEET_MANAGE)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('FLEET_MANAGE')")
    public ResponseEntity<ApiResponse<VehicleDetailResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVehicleStatusRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        VehicleDetailResponse response = vehicleService.updateStatus(id, request.getStatus(), currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái phương tiện thành công", response));
    }

    @Operation(summary = "Xóa phương tiện (Yêu cầu quyền FLEET_MANAGE)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FLEET_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(@PathVariable UUID id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa phương tiện thành công", null));
    }
}
