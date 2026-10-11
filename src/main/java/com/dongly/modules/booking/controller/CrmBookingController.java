package com.dongly.modules.booking.controller;

import com.dongly.common.api.ApiResponse;
import com.dongly.common.api.PageResponse;
import com.dongly.modules.booking.dto.CrmTripSearchCriteria;
import com.dongly.modules.booking.dto.CrmTripSearchResultResponse;
import com.dongly.modules.booking.dto.CrmTripSeatMapResponse;
import com.dongly.modules.booking.service.CrmBookingQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "CRM Booking", description = "Các endpoint tra cứu chuyến xe và sơ đồ ghế phục vụ CRM Booking")
@RestController
@RequestMapping("/api/v1/crm/bookings")
public class CrmBookingController {

    private final CrmBookingQueryService crmBookingQueryService;

    public CrmBookingController(CrmBookingQueryService crmBookingQueryService) {
        this.crmBookingQueryService = crmBookingQueryService;
    }

    @Operation(summary = "Tìm kiếm chuyến xe khả dụng cho CRM theo tuyến/ngày (Yêu cầu quyền BOOKING_READ)")
    @GetMapping("/trips/search")
    @PreAuthorize("hasAuthority('BOOKING_READ')")
    public ResponseEntity<PageResponse<CrmTripSearchResultResponse>> searchTrips(
            @Valid @ModelAttribute CrmTripSearchCriteria criteria
    ) {
        Page<CrmTripSearchResultResponse> pageResult = crmBookingQueryService.searchTrips(criteria);
        return ResponseEntity.ok(PageResponse.of(pageResult, "Tìm kiếm chuyến xe khả dụng thành công"));
    }

    @Operation(summary = "Lấy sơ đồ ghế và trạng thái đặt chỗ thời gian thực của chuyến xe (Yêu cầu quyền BOOKING_READ)")
    @GetMapping("/trips/{tripId}/seat-map")
    @PreAuthorize("hasAuthority('BOOKING_READ')")
    public ResponseEntity<ApiResponse<CrmTripSeatMapResponse>> getTripSeatMap(
            @PathVariable UUID tripId
    ) {
        CrmTripSeatMapResponse response = crmBookingQueryService.getTripSeatMap(tripId);
        return ResponseEntity.ok(ApiResponse.success("Lấy sơ đồ ghế chuyến xe thành công", response));
    }
}
