package com.dongly.modules.fleet.controller;

import com.dongly.common.api.ApiResponse;
import com.dongly.common.api.PageResponse;
import com.dongly.modules.fleet.dto.CreateDriverRequest;
import com.dongly.modules.fleet.dto.DriverResponse;
import com.dongly.modules.fleet.dto.UpdateDriverRequest;
import com.dongly.modules.fleet.dto.UpdateDriverStatusRequest;
import com.dongly.modules.fleet.entity.DriverStatus;
import com.dongly.modules.fleet.service.DriverService;
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

@Tag(name = "Driver Management", description = "Các endpoint quản trị hồ sơ và trạng thái tài xế")
@RestController
@RequestMapping("/api/v1/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @Operation(summary = "Lấy danh sách tài xế có phân trang, lọc và tìm kiếm (Yêu cầu quyền FLEET_READ)")
    @GetMapping
    @PreAuthorize("hasAuthority('FLEET_READ')")
    public ResponseEntity<PageResponse<DriverResponse>> searchDrivers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) DriverStatus status,
            @RequestParam(required = false) String licenseClass
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
        Page<DriverResponse> pageResult = driverService.searchDrivers(keyword, status, licenseClass, pageable);

        return ResponseEntity.ok(PageResponse.of(pageResult, "Lấy danh sách tài xế thành công"));
    }

    @Operation(summary = "Lấy chi tiết hồ sơ tài xế (Yêu cầu quyền FLEET_READ)")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FLEET_READ')")
    public ResponseEntity<ApiResponse<DriverResponse>> getDriverById(@PathVariable UUID id) {
        DriverResponse response = driverService.getDriverById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tài xế thành công", response));
    }

    @Operation(summary = "Tạo mới hồ sơ tài xế (Yêu cầu quyền FLEET_MANAGE)")
    @PostMapping
    @PreAuthorize("hasAuthority('FLEET_MANAGE')")
    public ResponseEntity<ApiResponse<DriverResponse>> createDriver(
            @Valid @RequestBody CreateDriverRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        DriverResponse response = driverService.createDriver(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo hồ sơ tài xế thành công", response));
    }

    @Operation(summary = "Cập nhật hồ sơ tài xế (Yêu cầu quyền FLEET_MANAGE)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FLEET_MANAGE')")
    public ResponseEntity<ApiResponse<DriverResponse>> updateDriver(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDriverRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        DriverResponse response = driverService.updateDriver(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ tài xế thành công", response));
    }

    @Operation(summary = "Cập nhật trạng thái tài xế (Yêu cầu quyền FLEET_MANAGE)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('FLEET_MANAGE')")
    public ResponseEntity<ApiResponse<DriverResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDriverStatusRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        DriverResponse response = driverService.updateStatus(id, request.getStatus(), currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái tài xế thành công", response));
    }

    @Operation(summary = "Xóa hồ sơ tài xế (Yêu cầu quyền FLEET_MANAGE)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FLEET_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteDriver(@PathVariable UUID id) {
        driverService.deleteDriver(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa hồ sơ tài xế thành công", null));
    }
}
