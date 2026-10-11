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
    private final com.dongly.modules.booking.service.CrmBookingTransactionService crmBookingTransactionService;

    public CrmBookingController(
            CrmBookingQueryService crmBookingQueryService,
            com.dongly.modules.booking.service.CrmBookingTransactionService crmBookingTransactionService
    ) {
        this.crmBookingQueryService = crmBookingQueryService;
        this.crmBookingTransactionService = crmBookingTransactionService;
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

    @Operation(summary = "Giữ tạm thời một hoặc nhiều ghế cho chuyến xe (Yêu cầu quyền BOOKING_MANAGE)")
    @org.springframework.web.bind.annotation.PostMapping("/hold")
    @PreAuthorize("hasAuthority('BOOKING_MANAGE')")
    public ResponseEntity<ApiResponse<com.dongly.modules.booking.dto.HoldSeatsResponse>> holdSeats(
            @Valid @org.springframework.web.bind.annotation.RequestBody com.dongly.modules.booking.dto.HoldSeatsRequest request
    ) {
        com.dongly.security.CurrentUser currentUser = com.dongly.security.SecurityUtils.getCurrentUser().orElse(null);
        com.dongly.modules.booking.dto.HoldSeatsResponse response = crmBookingTransactionService.holdSeats(request, currentUser);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.success("Giữ ghế thành công trong 10 phút", response));
    }

    @Operation(summary = "Xác nhận đơn đặt vé và xuất vé sau khi thu tiền hợp lệ (Yêu cầu quyền BOOKING_MANAGE)")
    @org.springframework.web.bind.annotation.PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('BOOKING_MANAGE')")
    public ResponseEntity<ApiResponse<com.dongly.modules.booking.dto.BookingResponse>> confirmBooking(
            @PathVariable UUID id,
            @Valid @org.springframework.web.bind.annotation.RequestBody com.dongly.modules.booking.dto.ConfirmBookingRequest request
    ) {
        com.dongly.security.CurrentUser currentUser = com.dongly.security.SecurityUtils.getCurrentUser().orElse(null);
        com.dongly.modules.booking.dto.BookingResponse response = crmBookingTransactionService.confirmBooking(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận đơn đặt vé thành công", response));
    }

    @Operation(summary = "Hủy giữ chỗ chủ động (Yêu cầu quyền BOOKING_MANAGE)")
    @org.springframework.web.bind.annotation.PostMapping("/{id}/cancel-hold")
    @PreAuthorize("hasAuthority('BOOKING_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> cancelHold(@PathVariable UUID id) {
        com.dongly.security.CurrentUser currentUser = com.dongly.security.SecurityUtils.getCurrentUser().orElse(null);
        crmBookingTransactionService.cancelHold(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy giữ chỗ thành công", null));
    }

    @Operation(summary = "Danh sách đơn đặt vé CRM phân trang và bộ lọc (Yêu cầu quyền BOOKING_READ)")
    @GetMapping
    @PreAuthorize("hasAuthority('BOOKING_READ')")
    public ResponseEntity<PageResponse<com.dongly.modules.booking.dto.BookingResponse>> searchBookings(
            @Valid @ModelAttribute com.dongly.modules.booking.dto.CrmBookingSearchCriteria criteria
    ) {
        Page<com.dongly.modules.booking.dto.BookingResponse> page = crmBookingQueryService.searchBookings(criteria);
        return ResponseEntity.ok(PageResponse.of(page, "Lấy danh sách đơn đặt vé thành công"));
    }

    @Operation(summary = "Xem chi tiết đơn đặt vé CRM theo ID (Yêu cầu quyền BOOKING_READ)")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BOOKING_READ')")
    public ResponseEntity<ApiResponse<com.dongly.modules.booking.dto.BookingResponse>> getBookingDetail(
            @PathVariable UUID id
    ) {
        com.dongly.modules.booking.dto.BookingResponse response = crmBookingQueryService.getBookingDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chi tiết đơn đặt vé thành công", response));
    }

    @Operation(summary = "Hủy đơn đặt vé theo quy định (Yêu cầu quyền BOOKING_MANAGE)")
    @org.springframework.web.bind.annotation.PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('BOOKING_MANAGE')")
    public ResponseEntity<ApiResponse<com.dongly.modules.booking.dto.BookingResponse>> cancelBooking(
            @PathVariable UUID id,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String reason
    ) {
        com.dongly.security.CurrentUser currentUser = com.dongly.security.SecurityUtils.getCurrentUser().orElse(null);
        com.dongly.modules.booking.dto.BookingResponse response = crmBookingTransactionService.cancelBooking(id, reason, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy đơn đặt vé thành công", response));
    }
}
