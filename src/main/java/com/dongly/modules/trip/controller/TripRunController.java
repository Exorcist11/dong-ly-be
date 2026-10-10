package com.dongly.modules.trip.controller;

import com.dongly.common.api.ApiResponse;
import com.dongly.common.api.PageResponse;
import com.dongly.modules.trip.dto.CreateTripRunRequest;
import com.dongly.modules.trip.dto.TripRunResponse;
import com.dongly.modules.trip.dto.UpdateTripRunRequest;
import com.dongly.modules.trip.dto.UpdateTripRunStatusRequest;
import com.dongly.modules.trip.entity.TripRunStatus;
import com.dongly.modules.trip.service.TripRunService;
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

@Tag(name = "TripRun Management", description = "Các endpoint quản trị cấu hình lịch vòng chạy định kỳ")
@RestController
@RequestMapping("/api/v1/trip-runs")
public class TripRunController {

    private final TripRunService tripRunService;

    public TripRunController(TripRunService tripRunService) {
        this.tripRunService = tripRunService;
    }

    @Operation(summary = "Lấy danh sách lịch vòng chạy có phân trang, lọc và tìm kiếm (Yêu cầu quyền TRIP_READ)")
    @GetMapping
    @PreAuthorize("hasAuthority('TRIP_READ')")
    public ResponseEntity<PageResponse<TripRunResponse>> searchTripRuns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID routeId,
            @RequestParam(required = false) TripRunStatus status
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
        Page<TripRunResponse> pageResult = tripRunService.searchTripRuns(keyword, routeId, status, pageable);

        return ResponseEntity.ok(PageResponse.of(pageResult, "Lấy danh sách lịch vòng chạy thành công"));
    }

    @Operation(summary = "Lấy chi tiết cấu hình lịch vòng chạy (Yêu cầu quyền TRIP_READ)")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TRIP_READ')")
    public ResponseEntity<ApiResponse<TripRunResponse>> getTripRunById(@PathVariable UUID id) {
        TripRunResponse response = tripRunService.getTripRunById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin lịch vòng chạy thành công", response));
    }

    @Operation(summary = "Tạo mới cấu hình lịch vòng chạy (Yêu cầu quyền TRIP_MANAGE)")
    @PostMapping
    @PreAuthorize("hasAuthority('TRIP_MANAGE')")
    public ResponseEntity<ApiResponse<TripRunResponse>> createTripRun(
            @Valid @RequestBody CreateTripRunRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        TripRunResponse response = tripRunService.createTripRun(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo lịch vòng chạy thành công", response));
    }

    @Operation(summary = "Cập nhật cấu hình lịch vòng chạy (Yêu cầu quyền TRIP_MANAGE)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TRIP_MANAGE')")
    public ResponseEntity<ApiResponse<TripRunResponse>> updateTripRun(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTripRunRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        TripRunResponse response = tripRunService.updateTripRun(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật lịch vòng chạy thành công", response));
    }

    @Operation(summary = "Cập nhật trạng thái kích hoạt của lịch vòng chạy (Yêu cầu quyền TRIP_MANAGE)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('TRIP_MANAGE')")
    public ResponseEntity<ApiResponse<TripRunResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTripRunStatusRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        TripRunResponse response = tripRunService.updateStatus(id, request.getStatus(), currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái lịch vòng chạy thành công", response));
    }

    @Operation(summary = "Xóa lịch vòng chạy chưa phát sinh chuyến (Yêu cầu quyền TRIP_MANAGE)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TRIP_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteTripRun(@PathVariable UUID id) {
        tripRunService.deleteTripRun(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa lịch vòng chạy thành công", null));
    }
}
