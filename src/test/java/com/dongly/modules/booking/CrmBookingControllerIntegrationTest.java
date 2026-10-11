package com.dongly.modules.booking;

import com.dongly.modules.booking.dto.CrmTripSearchResultResponse;
import com.dongly.modules.booking.dto.CrmTripSeatMapResponse;
import com.dongly.modules.booking.service.CrmBookingQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CrmBookingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CrmBookingQueryService crmBookingQueryService;

    @MockBean
    private com.dongly.modules.booking.service.CrmBookingTransactionService crmBookingTransactionService;

    @Test
    @DisplayName("Tìm kiếm chuyến xe khả dụng thành công khi có quyền BOOKING_READ")
    @WithMockUser(authorities = {"BOOKING_READ"})
    void searchTrips_Authorized_Returns200() throws Exception {
        CrmTripSearchResultResponse tripItem = CrmTripSearchResultResponse.builder()
                .tripId(UUID.randomUUID())
                .tripCode("TRP-20261015-0400")
                .routeName("Thanh Hóa - Hà Nội")
                .vehiclePlateNumber("36B-028.68")
                .totalSeats(22)
                .availableSeats(20)
                .basePrice(new BigDecimal("250000.00"))
                .departureTime(OffsetDateTime.now())
                .estimatedArrivalTime(OffsetDateTime.now().plusHours(3))
                .build();

        when(crmBookingQueryService.searchTrips(any()))
                .thenReturn(new PageImpl<>(List.of(tripItem)));

        mockMvc.perform(get("/api/v1/crm/bookings/trips/search")
                        .param("departureDate", "2026-10-15")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].tripCode").value("TRP-20261015-0400"))
                .andExpect(jsonPath("$.data.items[0].availableSeats").value(20));
    }

    @Test
    @DisplayName("Từ chối truy cập 403 khi người dùng không có quyền BOOKING_READ")
    @WithMockUser(authorities = {"OTHER_PERMISSION"})
    void searchTrips_Unauthorized_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/crm/bookings/trips/search")
                        .param("departureDate", "2026-10-15"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Báo lỗi 400 Validation khi thiếu tham số departureDate bắt buộc")
    @WithMockUser(authorities = {"BOOKING_READ"})
    void searchTrips_MissingDepartureDate_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/crm/bookings/trips/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Lấy sơ đồ ghế chuyến xe thành công khi có quyền BOOKING_READ")
    @WithMockUser(authorities = {"BOOKING_READ"})
    void getTripSeatMap_Authorized_Returns200() throws Exception {
        UUID tripId = UUID.randomUUID();
        CrmTripSeatMapResponse seatMap = CrmTripSeatMapResponse.builder()
                .tripId(tripId)
                .tripCode("TRP-20261015-0400")
                .totalSeats(22)
                .availableSeats(18)
                .heldSeats(2)
                .bookedSeats(2)
                .seats(List.of())
                .stops(List.of())
                .build();

        when(crmBookingQueryService.getTripSeatMap(tripId)).thenReturn(seatMap);

        mockMvc.perform(get("/api/v1/crm/bookings/trips/" + tripId + "/seat-map"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tripCode").value("TRP-20261015-0400"))
                .andExpect(jsonPath("$.data.availableSeats").value(18));
    }

    @Test
    @DisplayName("Giữ ghế thành công trả về 201 Created khi có quyền BOOKING_MANAGE")
    @WithMockUser(authorities = {"BOOKING_MANAGE"})
    void holdSeats_Authorized_Returns201() throws Exception {
        com.dongly.modules.booking.dto.HoldSeatsResponse holdResponse =
                com.dongly.modules.booking.dto.HoldSeatsResponse.builder()
                        .bookingId(UUID.randomUUID())
                        .bookingCode("DL-261011-0001")
                        .status(com.dongly.modules.booking.entity.BookingStatus.HELD)
                        .heldSeats(List.of("A01"))
                        .totalEstimatedAmount(new BigDecimal("300000.00"))
                        .holdExpiresAt(OffsetDateTime.now().plusMinutes(10))
                        .build();

        when(crmBookingTransactionService.holdSeats(any(), any())).thenReturn(holdResponse);

        String jsonPayload = """
            {
              "tripId": "%s",
              "seatIds": ["%s"],
              "customerPhone": "0912345678",
              "customerName": "Nguyễn Văn Nam"
            }
            """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/crm/bookings/hold")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingCode").value("DL-261011-0001"))
                .andExpect(jsonPath("$.data.status").value("HELD"));
    }

    @Test
    @DisplayName("Từ chối 403 khi gọi API hold mà không có quyền BOOKING_MANAGE")
    @WithMockUser(authorities = {"BOOKING_READ"})
    void holdSeats_Unauthorized_Returns403() throws Exception {
        String jsonPayload = """
            {
              "tripId": "%s",
              "seatIds": ["%s"],
              "customerPhone": "0912345678",
              "customerName": "Nguyễn Văn Nam"
            }
            """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/crm/bookings/hold")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isForbidden());
    }
}
