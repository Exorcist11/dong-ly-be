package com.dongly.modules.route.controller;

import com.dongly.common.api.ApiResponse;
import com.dongly.common.api.PageResponse;
import com.dongly.modules.route.dto.CreateRouteRequest;
import com.dongly.modules.route.dto.RouteDetailResponse;
import com.dongly.modules.route.dto.RouteSummaryResponse;
import com.dongly.modules.route.dto.UpdateRouteRequest;
import com.dongly.modules.route.dto.UpdateRouteStopsRequest;
import com.dongly.modules.route.dto.UpdateStatusRequest;
import com.dongly.modules.route.entity.CommonStatus;
import com.dongly.modules.route.service.RouteService;
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

@Tag(name = "Route Management", description = "Các endpoint quản trị tuyến đường và cấu hình trạm dừng")
@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @Operation(summary = "Lấy danh sách tuyến đường có phân trang, lọc và tìm kiếm (Yêu cầu quyền ROUTE_READ)")
    @GetMapping
    @PreAuthorize("hasAuthority('ROUTE_READ')")
    public ResponseEntity<PageResponse<RouteSummaryResponse>> searchRoutes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID originLocationId,
            @RequestParam(required = false) UUID destinationLocationId,
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
        Page<RouteSummaryResponse> pageResult = routeService.searchRoutes(
                keyword, originLocationId, destinationLocationId, status, pageable);

        return ResponseEntity.ok(PageResponse.of(pageResult, "Lấy danh sách tuyến đường thành công"));
    }

    @Operation(summary = "Lấy chi tiết tuyến đường bao gồm danh sách điểm dừng (Yêu cầu quyền ROUTE_READ)")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROUTE_READ')")
    public ResponseEntity<ApiResponse<RouteDetailResponse>> getRouteById(@PathVariable UUID id) {
        RouteDetailResponse response = routeService.getRouteById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tuyến đường thành công", response));
    }

    @Operation(summary = "Tạo mới tuyến đường (Yêu cầu quyền ROUTE_MANAGE)")
    @PostMapping
    @PreAuthorize("hasAuthority('ROUTE_MANAGE')")
    public ResponseEntity<ApiResponse<RouteDetailResponse>> createRoute(
            @Valid @RequestBody CreateRouteRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        RouteDetailResponse response = routeService.createRoute(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo tuyến đường thành công", response));
    }

    @Operation(summary = "Cập nhật thông tin cơ bản của tuyến đường (Yêu cầu quyền ROUTE_MANAGE)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROUTE_MANAGE')")
    public ResponseEntity<ApiResponse<RouteDetailResponse>> updateRoute(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRouteRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        RouteDetailResponse response = routeService.updateRoute(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin tuyến đường thành công", response));
    }

    @Operation(summary = "Kích hoạt hoặc ngừng hoạt động tuyến đường (Yêu cầu quyền ROUTE_MANAGE)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROUTE_MANAGE')")
    public ResponseEntity<ApiResponse<RouteDetailResponse>> updateRouteStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        RouteDetailResponse response = routeService.updateRouteStatus(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái tuyến đường thành công", response));
    }

    @Operation(summary = "Cập nhật toàn bộ danh sách điểm dừng và thứ tự của tuyến theo lô (Yêu cầu quyền ROUTE_MANAGE)")
    @PutMapping("/{id}/stops")
    @PreAuthorize("hasAuthority('ROUTE_MANAGE')")
    public ResponseEntity<ApiResponse<RouteDetailResponse>> updateRouteStops(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRouteStopsRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        RouteDetailResponse response = routeService.updateRouteStops(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh sách điểm dừng thành công", response));
    }
}
