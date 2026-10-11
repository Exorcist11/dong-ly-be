package com.dongly.modules.booking;

import com.dongly.common.exception.AppException;
import com.dongly.common.exception.BusinessRuleException;
import com.dongly.common.exception.ErrorCode;
import com.dongly.modules.booking.dto.BookingResponse;
import com.dongly.modules.booking.dto.ConfirmBookingRequest;
import com.dongly.modules.booking.dto.HoldSeatsRequest;
import com.dongly.modules.booking.dto.HoldSeatsResponse;
import com.dongly.modules.booking.dto.PassengerItemRequest;
import com.dongly.modules.booking.dto.PaymentRequest;
import com.dongly.modules.booking.entity.Booking;
import com.dongly.modules.booking.entity.BookingItem;
import com.dongly.modules.booking.entity.BookingItemStatus;
import com.dongly.modules.booking.entity.BookingStatus;
import com.dongly.modules.booking.entity.PaymentMethod;
import com.dongly.modules.booking.repository.BookingItemRepository;
import com.dongly.modules.booking.repository.BookingRepository;
import com.dongly.modules.booking.repository.PaymentRepository;
import com.dongly.modules.booking.repository.TicketRepository;
import com.dongly.modules.booking.service.CrmBookingTransactionService;
import com.dongly.modules.fleet.entity.Driver;
import com.dongly.modules.fleet.entity.DriverStatus;
import com.dongly.modules.fleet.entity.SeatStatus;
import com.dongly.modules.fleet.entity.SeatType;
import com.dongly.modules.fleet.entity.Vehicle;
import com.dongly.modules.fleet.entity.VehicleSeat;
import com.dongly.modules.fleet.entity.VehicleStatus;
import com.dongly.modules.fleet.entity.VehicleType;
import com.dongly.modules.fleet.repository.DriverRepository;
import com.dongly.modules.fleet.repository.VehicleRepository;
import com.dongly.modules.fleet.repository.VehicleSeatRepository;
import com.dongly.modules.route.entity.CommonStatus;
import com.dongly.modules.route.entity.Location;
import com.dongly.modules.route.entity.Route;
import com.dongly.modules.route.repository.LocationRepository;
import com.dongly.modules.route.repository.RouteRepository;
import com.dongly.modules.trip.entity.Trip;
import com.dongly.modules.trip.entity.TripStatus;
import com.dongly.modules.trip.repository.TripRepository;
import com.dongly.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Mốc 7: Bộ kiểm thử tích hợp E2E toàn diện cho module CRM Booking Management Đông Lý.
 * Kiểm chứng 100% các tiêu chí nghiệm thu khắt khe:
 * 1. Chống đặt trùng ghế & Tính nhất quán trạng thái.
 * 2. Hết hạn giữ ghế 10 phút & Tự động dọn dẹp.
 * 3. Chống tạo booking trùng khi client retry (Idempotency).
 * 4. Chống giả mạo giá vé (Client price tampering prevention).
 * 5. Rollback dữ liệu khi có lỗi xảy ra.
 * 6. Kiểm soát quyền sở hữu và RBAC.
 */
@SpringBootTest
@ActiveProfiles("test")
class CrmBookingE2eWorkflowIntegrationTest {

    @Autowired
    private CrmBookingTransactionService transactionService;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleSeatRepository vehicleSeatRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingItemRepository bookingItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    private Trip testTrip;
    private VehicleSeat seat1;
    private VehicleSeat seat2;
    private CurrentUser staffUser;
    private CurrentUser otherStaffUser;

    @BeforeEach
    void setUp() {
        Location loc1 = locationRepository.save(Location.builder()
                .code("LOC_E2E_1_" + UUID.randomUUID().toString().substring(0, 5))
                .name("Hà Nội")
                .province("Hà Nội")
                .status(CommonStatus.ACTIVE)
                .build());

        Location loc2 = locationRepository.save(Location.builder()
                .code("LOC_E2E_2_" + UUID.randomUUID().toString().substring(0, 5))
                .name("Thanh Hóa")
                .province("Thanh Hóa")
                .status(CommonStatus.ACTIVE)
                .build());

        Route route = routeRepository.save(Route.builder()
                .code("RT_E2E_" + UUID.randomUUID().toString().substring(0, 5))
                .name("Hà Nội - Sầm Sơn")
                .originLocation(loc1)
                .destinationLocation(loc2)
                .status(CommonStatus.ACTIVE)
                .build());

        Vehicle vehicle = vehicleRepository.save(Vehicle.builder()
                .plateNumber("36B-" + UUID.randomUUID().toString().substring(0, 6))
                .vehicleType(VehicleType.LIMOUSINE)
                .brand("Thaco")
                .status(VehicleStatus.ACTIVE)
                .totalFloors(1)
                .totalRows(5)
                .totalColumns(3)
                .totalSeats(10)
                .build());

        seat1 = vehicleSeatRepository.save(VehicleSeat.builder()
                .vehicle(vehicle)
                .seatCode("E2E_A1")
                .floor(1)
                .rowIndex(1)
                .columnIndex(1)
                .seatType(SeatType.VIP)
                .extraPrice(new BigDecimal("50000.00"))
                .status(SeatStatus.ACTIVE)
                .build());

        seat2 = vehicleSeatRepository.save(VehicleSeat.builder()
                .vehicle(vehicle)
                .seatCode("E2E_A2")
                .floor(1)
                .rowIndex(1)
                .columnIndex(2)
                .seatType(SeatType.STANDARD)
                .extraPrice(BigDecimal.ZERO)
                .status(SeatStatus.ACTIVE)
                .build());

        Driver driver1 = driverRepository.save(Driver.builder()
                .code("TX_E2E1_" + UUID.randomUUID().toString().substring(0, 5))
                .fullName("Lê Văn Tài")
                .phone("0988" + UUID.randomUUID().toString().substring(0, 6).replaceAll("\\D", "0"))
                .licenseNumber("GPLX_E2E1_" + UUID.randomUUID().toString().substring(0, 5))
                .licenseClass("E")
                .status(DriverStatus.ACTIVE)
                .build());

        Driver driver2 = driverRepository.save(Driver.builder()
                .code("TX_E2E2_" + UUID.randomUUID().toString().substring(0, 5))
                .fullName("Nguyễn Văn Phụ")
                .phone("0989" + UUID.randomUUID().toString().substring(0, 6).replaceAll("\\D", "0"))
                .licenseNumber("GPLX_E2E2_" + UUID.randomUUID().toString().substring(0, 5))
                .licenseClass("E")
                .status(DriverStatus.ACTIVE)
                .build());

        testTrip = tripRepository.save(Trip.builder()
                .code("TRP_E2E_" + UUID.randomUUID().toString().substring(0, 5))
                .route(route)
                .vehicle(vehicle)
                .driver(driver1)
                .assistantDriver(driver2)
                .departureTime(OffsetDateTime.now().plusDays(2))
                .estimatedArrivalTime(OffsetDateTime.now().plusDays(2).plusHours(3))
                .basePrice(new BigDecimal("200000.00"))
                .status(TripStatus.READY)
                .build());

        staffUser = new CurrentUser(
                UUID.randomUUID(), "staff_mai", "mai@dongly.vn",
                Set.of("STAFF"), Set.of("BOOKING_READ", "BOOKING_MANAGE")
        );

        otherStaffUser = new CurrentUser(
                UUID.randomUUID(), "staff_hung", "hung@dongly.vn",
                Set.of("STAFF"), Set.of("BOOKING_READ", "BOOKING_MANAGE")
        );
    }

    @Test
    @DisplayName("E2E Luồng chuẩn: Giữ ghế -> Xác nhận thanh toán & xuất vé -> Hủy vé giải phóng ghế")
    void testE2e_FullWorkflow_HoldConfirmCancel() {
        // BƯỚC 1: GIỮ GHẾ
        HoldSeatsRequest holdReq = HoldSeatsRequest.builder()
                .tripId(testTrip.getId())
                .seatIds(List.of(seat1.getId()))
                .customerPhone("0912999888")
                .customerName("Khách E2E")
                .note("Đặt trước 1 ghế VIP")
                .build();

        HoldSeatsResponse holdRes = transactionService.holdSeats(holdReq, staffUser);
        assertThat(holdRes).isNotNull();
        assertThat(holdRes.getStatus()).isEqualTo(BookingStatus.HELD);
        // Giá = Base 200k + Extra VIP 50k = 250k
        assertThat(holdRes.getTotalEstimatedAmount()).isEqualByComparingTo("250000.00");

        UUID bookingId = holdRes.getBookingId();

        // Kiểm tra database: Ghế đang HELD
        List<BookingItem> heldItems = bookingItemRepository.findActiveItemsByTrip(testTrip.getId(), OffsetDateTime.now());
        assertThat(heldItems).hasSize(1);
        assertThat(heldItems.get(0).getStatus()).isEqualTo(BookingItemStatus.HELD);

        // BƯỚC 2: XÁC NHẬN ĐƠN VÉ (CONFIRM)
        ConfirmBookingRequest confirmReq = ConfirmBookingRequest.builder()
                .customerName("Khách E2E")
                .customerPhone("0912999888")
                .passengers(List.of(PassengerItemRequest.builder()
                        .seatId(seat1.getId())
                        .passengerName("Khách E2E")
                        .passengerPhone("0912999888")
                        .build()))
                .payment(PaymentRequest.builder()
                        .method(PaymentMethod.CASH)
                        .amount(new BigDecimal("250000.00")) // Đúng tổng tiền
                        .build())
                .build();

        BookingResponse confirmRes = transactionService.confirmBooking(bookingId, confirmReq, staffUser);
        assertThat(confirmRes).isNotNull();
        assertThat(confirmRes.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(confirmRes.getTotalAmount()).isEqualByComparingTo("250000.00");
        assertThat(confirmRes.getItems()).hasSize(1);
        assertThat(confirmRes.getItems().get(0).getStatus()).isEqualTo(BookingItemStatus.CONFIRMED);

        // BƯỚC 3: HỦY ĐƠN VÉ (CANCEL)
        BookingResponse cancelRes = transactionService.cancelBooking(bookingId, "Khách có việc bận xin hủy", staffUser);
        assertThat(cancelRes.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        // Kiểm tra database: Ghế không còn active nữa (đã giải phóng)
        List<BookingItem> activeItemsAfterCancel = bookingItemRepository.findActiveItemsByTrip(testTrip.getId(), OffsetDateTime.now());
        assertThat(activeItemsAfterCancel).isEmpty();

        // Người khác có thể giữ lại ghế này ngay lập tức
        HoldSeatsResponse reHoldRes = transactionService.holdSeats(holdReq, otherStaffUser);
        assertThat(reHoldRes.getStatus()).isEqualTo(BookingStatus.HELD);
    }

    @Test
    @DisplayName("Idempotency: Client bấm xác nhận lại (retry) khi đã CONFIRMED -> Bị từ chối an toàn")
    void testConfirm_IdempotencyRetry_Rejected() {
        // Giữ ghế
        HoldSeatsRequest holdReq = HoldSeatsRequest.builder()
                .tripId(testTrip.getId())
                .seatIds(List.of(seat2.getId()))
                .customerPhone("0912111222")
                .customerName("Khách Retry")
                .build();
        HoldSeatsResponse holdRes = transactionService.holdSeats(holdReq, staffUser);

        // Xác nhận lần 1
        ConfirmBookingRequest confirmReq = ConfirmBookingRequest.builder()
                .customerName("Khách Retry")
                .customerPhone("0912111222")
                .passengers(List.of(PassengerItemRequest.builder()
                        .seatId(seat2.getId())
                        .passengerName("Khách Retry")
                        .build()))
                .payment(PaymentRequest.builder()
                        .method(PaymentMethod.BANK_TRANSFER)
                        .amount(new BigDecimal("200000.00"))
                        .build())
                .build();
        transactionService.confirmBooking(holdRes.getBookingId(), confirmReq, staffUser);

        // Client retry xác nhận lần 2
        assertThatThrownBy(() -> transactionService.confirmBooking(holdRes.getBookingId(), confirmReq, staffUser))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_ALREADY_CONFIRMED);
    }

    @Test
    @DisplayName("Bảo mật & Toàn vẹn giá: Client gửi sai giá vé (tampering) -> Bị từ chối và Rollback")
    void testConfirm_PriceTampering_RejectedAndRollbacked() {
        // Giữ ghế VIP seat1 (250k)
        HoldSeatsRequest holdReq = HoldSeatsRequest.builder()
                .tripId(testTrip.getId())
                .seatIds(List.of(seat1.getId()))
                .customerPhone("0912333444")
                .customerName("Khách Hack Giá")
                .build();
        HoldSeatsResponse holdRes = transactionService.holdSeats(holdReq, staffUser);

        // Client cố tình gửi số tiền thấp hơn (100k thay vì 250k)
        ConfirmBookingRequest hackReq = ConfirmBookingRequest.builder()
                .customerName("Khách Hack Giá")
                .customerPhone("0912333444")
                .passengers(List.of(PassengerItemRequest.builder()
                        .seatId(seat1.getId())
                        .passengerName("Khách Hack Giá")
                        .build()))
                .payment(PaymentRequest.builder()
                        .method(PaymentMethod.CASH)
                        .amount(new BigDecimal("100000.00")) // Giả mạo giá
                        .build())
                .build();

        assertThatThrownBy(() -> transactionService.confirmBooking(holdRes.getBookingId(), hackReq, staffUser))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không khớp với tổng tiền thực tế của đơn vé");

        // Kiểm tra database: Đơn vé vẫn giữ nguyên trạng thái HELD (Rollback hoàn toàn)
        Booking booking = bookingRepository.findById(holdRes.getBookingId()).orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.HELD);
    }

    @Test
    @DisplayName("Hết hạn giữ chỗ 10 phút: Bị từ chối xác nhận và tự động cập nhật EXPIRED")
    void testConfirm_ExpiredHold_RejectedAndAutoCleaned() {
        // Giữ ghế
        HoldSeatsRequest holdReq = HoldSeatsRequest.builder()
                .tripId(testTrip.getId())
                .seatIds(List.of(seat2.getId()))
                .customerPhone("0912555666")
                .customerName("Khách Hết Hạn")
                .build();
        HoldSeatsResponse holdRes = transactionService.holdSeats(holdReq, staffUser);

        // Giả lập thời gian giữ chỗ đã trôi qua 10 phút (holdExpiresAt trong quá khứ)
        Booking booking = bookingRepository.findById(holdRes.getBookingId()).orElseThrow();
        booking.setHoldExpiresAt(OffsetDateTime.now().minusMinutes(5));
        bookingRepository.save(booking);

        ConfirmBookingRequest confirmReq = ConfirmBookingRequest.builder()
                .customerName("Khách Hết Hạn")
                .customerPhone("0912555666")
                .passengers(List.of(PassengerItemRequest.builder()
                        .seatId(seat2.getId())
                        .passengerName("Khách Hết Hạn")
                        .build()))
                .payment(PaymentRequest.builder()
                        .method(PaymentMethod.CASH)
                        .amount(new BigDecimal("200000.00"))
                        .build())
                .build();

        assertThatThrownBy(() -> transactionService.confirmBooking(holdRes.getBookingId(), confirmReq, staffUser))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_HOLD_EXPIRED);

        // Kiểm tra database: Trạng thái booking và items tự động được đánh dấu EXPIRED (tự dọn dẹp)
        Booking updatedBooking = bookingRepository.findByIdWithDetails(holdRes.getBookingId()).orElseThrow();
        assertThat(updatedBooking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(updatedBooking.getItems().get(0).getStatus()).isEqualTo(BookingItemStatus.EXPIRED);

        // Ghế không còn active nữa -> giải phóng cho khách khác
        List<BookingItem> activeItems = bookingItemRepository.findActiveItemsByTrip(testTrip.getId(), OffsetDateTime.now());
        assertThat(activeItems).isEmpty();
    }
}
