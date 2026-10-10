package com.dongly.modules.trip.controller;

import com.dongly.common.api.ApiResponse;
import com.dongly.common.api.PageResponse;
import com.dongly.modules.trip.dto.ConflictCheckRequest;
import com.dongly.modules.trip.dto.ConflictCheckResponse;
import com.dongly.modules.trip.dto.CreateTripRequest;
import com.dongly.modules.trip.dto.GenerateTripsPreviewResponse;
import com.dongly.modules.trip.dto.GenerateTripsRequest;
import com.dongly.modules.trip.dto.GenerateTripsResultResponse;
import com.dongly.modules.trip.dto.TripResponse;
import com.dongly.modules.trip.dto.UpdateTripRequest;
import com.dongly.modules.trip.dto.UpdateTripStatusRequest;
import com.dongly.modules.trip.entity.TripStatus;
import com.dongly.modules.trip.service.TripGeneratorService;
import com.dongly.modules.trip.service.TripService;
import com.dongly.security.CurrentUser;
import com.dongly.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@Tag(name = "Trip Management", description = "Các endpoint điều hành và quản lý chuyến xe thực tế")
@RestController
@RequestMapping("/api/v1/trips")
public class TripController {

    private final TripService tripService;
    private final TripGeneratorService tripGeneratorService;

    public TripController(TripService tripService, TripGeneratorService tripGeneratorService) {
        this.tripService = tripService;
        this.tripGeneratorService = tripGeneratorService;
    }

    @Operation(summary = "Lấy danh sách chuyến xe có phân trang, lọc và tìm kiếm (Yêu cầu quyền TRIP_READ)")
    @GetMapping
    @PreAuthorize("hasAuthority('TRIP_READ')")
    public ResponseEntity<PageResponse<TripResponse>> searchTrips(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "departureTime,asc") String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID routeId,
            @RequestParam(required = false) UUID vehicleId,
            @RequestParam(required = false) UUID driverId,
            @RequestParam(required = false) TripStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate
    ) {
        if (size > 100) {
            size = 100;
        }

        Sort sortObj = Sort.by(Sort.Direction.ASC, "departureTime");
        if (sort != null && sort.contains(",")) {
            String[] parts = sort.split(",");
            Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("desc")
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            sortObj = Sort.by(direction, parts[0]);
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), size, sortObj);
        Page<TripResponse> pageResult = tripService.searchTrips(
                keyword, routeId, vehicleId, driverId, status, fromDate, toDate, pageable
        );

        return ResponseEntity.ok(PageResponse.of(pageResult, "Lấy danh sách chuyến xe thành công"));
    }

    @Operation(summary = "Lấy chi tiết chuyến xe bao gồm phương tiện và 2 tài xế (Yêu cầu quyền TRIP_READ)")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TRIP_READ')")
    public ResponseEntity<ApiResponse<TripResponse>> getTripById(@PathVariable UUID id) {
        TripResponse response = tripService.getTripById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chuyến xe thành công", response));
    }

    @Operation(summary = "Tạo mới chuyến xe tăng cường hoặc chuyến thủ công (Yêu cầu quyền TRIP_MANAGE)")
    @PostMapping
    @PreAuthorize("hasAuthority('TRIP_MANAGE')")
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(
            @Valid @RequestBody CreateTripRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        TripResponse response = tripService.createTrip(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo chuyến xe thành công", response));
    }

    @Operation(summary = "Cập nhật thông tin chuyến xe (Yêu cầu quyền TRIP_MANAGE)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TRIP_MANAGE')")
    public ResponseEntity<ApiResponse<TripResponse>> updateTrip(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTripRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        TripResponse response = tripService.updateTrip(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật chuyến xe thành công", response));
    }

    @Operation(summary = "Chuyển trạng thái vận hành của chuyến xe (Yêu cầu quyền TRIP_MANAGE)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('TRIP_MANAGE')")
    public ResponseEntity<ApiResponse<TripResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTripStatusRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        TripResponse response = tripService.updateStatus(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái chuyến xe thành công", response));
    }

    @Operation(summary = "Xem trước danh sách chuyến dự kiến sinh từ lịch vòng chạy (Yêu cầu quyền TRIP_MANAGE)")
    @PostMapping("/preview-generate")
    @PreAuthorize("hasAuthority('TRIP_MANAGE')")
    public ResponseEntity<ApiResponse<GenerateTripsPreviewResponse>> previewGenerate(
            @Valid @RequestBody GenerateTripsRequest request
    ) {
        GenerateTripsPreviewResponse response = tripGeneratorService.previewGenerateTrips(request);
        return ResponseEntity.ok(ApiResponse.success("Xem trước danh sách chuyến dự kiến thành công", response));
    }

    @Operation(summary = "Xác nhận tự động sinh chuyến xe từ lịch vòng chạy vào cơ sở dữ liệu (Yêu cầu quyền TRIP_MANAGE)")
    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('TRIP_MANAGE')")
    public ResponseEntity<ApiResponse<GenerateTripsResultResponse>> executeGenerate(
            @Valid @RequestBody GenerateTripsRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        GenerateTripsResultResponse response = tripGeneratorService.executeGenerateTrips(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response.getMessage(), response));
    }

    @Operation(summary = "Kiểm tra xung đột phân công phương tiện và tài xế (Yêu cầu quyền TRIP_READ)")
    @PostMapping("/check-conflict")
    @PreAuthorize("hasAuthority('TRIP_READ')")
    public ResponseEntity<ApiResponse<ConflictCheckResponse>> checkConflict(
            @Valid @RequestBody ConflictCheckRequest request
    ) {
        ConflictCheckResponse response = tripService.checkConflict(request);
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra xung đột phân công thành công", response));
    }
}
