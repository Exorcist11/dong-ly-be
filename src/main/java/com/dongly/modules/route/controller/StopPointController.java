package com.dongly.modules.route.controller;

import com.dongly.common.api.ApiResponse;
import com.dongly.common.api.PageResponse;
import com.dongly.modules.route.dto.CreateStopPointRequest;
import com.dongly.modules.route.dto.StopPointResponse;
import com.dongly.modules.route.dto.UpdateStatusRequest;
import com.dongly.modules.route.dto.UpdateStopPointRequest;
import com.dongly.modules.route.entity.CommonStatus;
import com.dongly.modules.route.service.StopPointService;
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

@Tag(name = "Stop Point Management", description = "Các endpoint quản lý danh mục điểm đón/trả vật lý độc lập")
@RestController
@RequestMapping("/api/v1/stop-points")
public class StopPointController {

    private final StopPointService stopPointService;

    public StopPointController(StopPointService stopPointService) {
        this.stopPointService = stopPointService;
    }

    @Operation(summary = "Lấy danh sách điểm đón/trả có phân trang, lọc và tìm kiếm (Yêu cầu quyền ROUTE_READ)")
    @GetMapping
    @PreAuthorize("hasAuthority('ROUTE_READ')")
    public ResponseEntity<PageResponse<StopPointResponse>> searchStopPoints(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) CommonStatus status
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
        Page<StopPointResponse> pageResult = stopPointService.searchStopPoints(keyword, locationId, status, pageable);

        return ResponseEntity.ok(PageResponse.of(pageResult, "Lấy danh sách điểm đón/trả thành công"));
    }

    @Operation(summary = "Lấy chi tiết điểm đón/trả theo ID (Yêu cầu quyền ROUTE_READ)")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROUTE_READ')")
    public ResponseEntity<ApiResponse<StopPointResponse>> getStopPointById(@PathVariable UUID id) {
        StopPointResponse response = stopPointService.getStopPointById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin điểm đón/trả thành công", response));
    }

    @Operation(summary = "Tạo mới điểm đón/trả (Yêu cầu quyền ROUTE_MANAGE)")
    @PostMapping
    @PreAuthorize("hasAuthority('ROUTE_MANAGE')")
    public ResponseEntity<ApiResponse<StopPointResponse>> createStopPoint(
            @Valid @RequestBody CreateStopPointRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        StopPointResponse response = stopPointService.createStopPoint(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo điểm đón/trả thành công", response));
    }

    @Operation(summary = "Cập nhật thông tin điểm đón/trả (Yêu cầu quyền ROUTE_MANAGE)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROUTE_MANAGE')")
    public ResponseEntity<ApiResponse<StopPointResponse>> updateStopPoint(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStopPointRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        StopPointResponse response = stopPointService.updateStopPoint(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật điểm đón/trả thành công", response));
    }

    @Operation(summary = "Kích hoạt hoặc ngừng hoạt động điểm đón/trả (Yêu cầu quyền ROUTE_MANAGE)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROUTE_MANAGE')")
    public ResponseEntity<ApiResponse<StopPointResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        StopPointResponse response = stopPointService.updateStatus(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái điểm đón/trả thành công", response));
    }
}
