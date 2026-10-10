package com.dongly.modules.route.controller;

import com.dongly.common.api.ApiResponse;
import com.dongly.modules.route.dto.LocationResponse;
import com.dongly.modules.route.service.StopPointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Location Management", description = "Các endpoint tra cứu danh mục địa phương / Tỉnh thành khai thác")
@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final StopPointService stopPointService;

    public LocationController(StopPointService stopPointService) {
        this.stopPointService = stopPointService;
    }

    @Operation(summary = "Lấy danh mục các địa phương đang hoạt động")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getActiveLocations() {
        List<LocationResponse> locations = stopPointService.getActiveLocations();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách địa phương thành công", locations));
    }
}
