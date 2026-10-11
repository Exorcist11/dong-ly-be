package com.dongly.modules.booking;

import com.dongly.common.exception.AppException;
import com.dongly.common.exception.BusinessRuleException;
import com.dongly.common.exception.ErrorCode;
import com.dongly.common.exception.ResourceNotFoundException;
import com.dongly.modules.booking.dto.BookingResponse;
import com.dongly.modules.booking.dto.ConfirmBookingRequest;
import com.dongly.modules.booking.dto.HoldSeatsRequest;
import com.dongly.modules.booking.dto.HoldSeatsResponse;
import com.dongly.modules.booking.dto.PassengerItemRequest;
import com.dongly.modules.booking.dto.PaymentRequest;
import com.dongly.modules.booking.entity.Booking;
import com.dongly.modules.booking.entity.BookingChannel;
import com.dongly.modules.booking.entity.BookingItem;
import com.dongly.modules.booking.entity.BookingItemStatus;
import com.dongly.modules.booking.entity.BookingStatus;
import com.dongly.modules.booking.entity.Customer;
import com.dongly.modules.booking.entity.PaymentMethod;
import com.dongly.modules.booking.repository.BookingItemRepository;
import com.dongly.modules.booking.repository.BookingRepository;
import com.dongly.modules.booking.repository.CustomerRepository;
import com.dongly.modules.booking.repository.PaymentRepository;
import com.dongly.modules.booking.repository.TicketRepository;
import com.dongly.modules.booking.service.CrmBookingTransactionService;
import com.dongly.modules.fleet.entity.SeatStatus;
import com.dongly.modules.fleet.entity.SeatType;
import com.dongly.modules.fleet.entity.Vehicle;
import com.dongly.modules.fleet.entity.VehicleSeat;
import com.dongly.modules.fleet.repository.VehicleSeatRepository;
import com.dongly.modules.route.entity.Route;
import com.dongly.modules.route.repository.RouteStopRepository;
import com.dongly.modules.trip.entity.Trip;
import com.dongly.modules.trip.entity.TripStatus;
import com.dongly.modules.trip.repository.TripRepository;
import com.dongly.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmBookingTransactionServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private VehicleSeatRepository vehicleSeatRepository;

    @Mock
    private RouteStopRepository routeStopRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingItemRepository bookingItemRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private CrmBookingTransactionService transactionService;

    private Trip trip;
    private Vehicle vehicle;
    private VehicleSeat seatA01;
    private VehicleSeat seatA02;
    private CurrentUser staffUser;
    private CurrentUser otherStaffUser;

    @BeforeEach
    void setUp() {
        vehicle = Vehicle.builder()
                .id(UUID.randomUUID())
                .plateNumber("36B-028.68")
                .build();

        seatA01 = VehicleSeat.builder()
                .id(UUID.randomUUID())
                .vehicle(vehicle)
                .seatCode("A01")
                .seatType(SeatType.LUXURY_ROOM)
                .extraPrice(new BigDecimal("50000.00"))
                .status(SeatStatus.ACTIVE)
                .build();

        seatA02 = VehicleSeat.builder()
                .id(UUID.randomUUID())
                .vehicle(vehicle)
                .seatCode("A02")
                .seatType(SeatType.LUXURY_ROOM)
                .extraPrice(new BigDecimal("50000.00"))
                .status(SeatStatus.ACTIVE)
                .build();

        trip = Trip.builder()
                .id(UUID.randomUUID())
                .code("TRP-20261015-0400")
                .vehicle(vehicle)
                .basePrice(new BigDecimal("250000.00"))
                .status(TripStatus.READY)
                .departureTime(OffsetDateTime.now(ZoneOffset.ofHours(7)).plusDays(1))
                .build();

        staffUser = new CurrentUser(UUID.randomUUID(), "staff_hoa", "hoa@dongly.vn", Set.of("STAFF"), Set.of("BOOKING_MANAGE"));
        otherStaffUser = new CurrentUser(UUID.randomUUID(), "staff_nam", "nam@dongly.vn", Set.of("STAFF"), Set.of("BOOKING_MANAGE"));
    }

    @Test
    @DisplayName("Giữ ghế thành công: Thiết lập thời hạn 10 phút, trạng thái HELD và tính tổng tiền")
    void holdSeats_Success() {
        HoldSeatsRequest request = HoldSeatsRequest.builder()
                .tripId(trip.getId())
                .seatIds(List.of(seatA01.getId(), seatA02.getId()))
                .customerPhone("0912345678")
                .customerName("Nguyễn Văn Khách")
                .note("Khách gọi đặt 2 chỗ")
                .build();

        when(tripRepository.findByIdForUpdate(trip.getId())).thenReturn(Optional.of(trip));
        when(vehicleSeatRepository.findAllById(request.getSeatIds())).thenReturn(List.of(seatA01, seatA02));
        when(bookingItemRepository.findConflictingSeats(any(), any(), any())).thenReturn(Collections.emptyList());
        when(customerRepository.findByPhone("0912345678")).thenReturn(Optional.of(Customer.builder()
                .id(UUID.randomUUID())
                .phone("0912345678")
                .fullName("Nguyễn Văn Khách")
                .build()));

        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> {
            Booking b = i.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        HoldSeatsResponse response = transactionService.holdSeats(request, staffUser);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BookingStatus.HELD);
        assertThat(response.getHeldSeats()).containsExactlyInAnyOrder("A01", "A02");
        // 2 ghế * (250k base + 50k extra) = 600k
        assertThat(response.getTotalEstimatedAmount()).isEqualByComparingTo("600000.00");
        assertThat(response.getHoldExpiresAt()).isAfter(OffsetDateTime.now());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("Ném SEAT_ALREADY_RESERVED khi ghế đang có người giữ hoặc đặt trên cùng chuyến xe")
    void holdSeats_ThrowsException_WhenSeatAlreadyReserved() {
        HoldSeatsRequest request = HoldSeatsRequest.builder()
                .tripId(trip.getId())
                .seatIds(List.of(seatA01.getId()))
                .customerPhone("0912345678")
                .customerName("Nguyễn Văn Khách")
                .build();

        when(tripRepository.findByIdForUpdate(trip.getId())).thenReturn(Optional.of(trip));
        when(vehicleSeatRepository.findAllById(request.getSeatIds())).thenReturn(List.of(seatA01));

        BookingItem conflictingItem = BookingItem.builder()
                .seatCode("A01")
                .status(BookingItemStatus.HELD)
                .build();
        when(bookingItemRepository.findConflictingSeats(any(), any(), any())).thenReturn(List.of(conflictingItem));

        assertThatThrownBy(() -> transactionService.holdSeats(request, staffUser))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_ALREADY_RESERVED);
    }

    @Test
    @DisplayName("Xử lý an toàn khi DataIntegrityViolationException xảy ra từ Database Constraint chống giữ trùng")
    void holdSeats_CatchesDatabaseConstraintViolation_AndThrowsFriendlyError() {
        HoldSeatsRequest request = HoldSeatsRequest.builder()
                .tripId(trip.getId())
                .seatIds(List.of(seatA01.getId()))
                .customerPhone("0912345678")
                .customerName("Nguyễn Văn Khách")
                .build();

        when(tripRepository.findByIdForUpdate(trip.getId())).thenReturn(Optional.of(trip));
        when(vehicleSeatRepository.findAllById(request.getSeatIds())).thenReturn(List.of(seatA01));
        when(bookingItemRepository.findConflictingSeats(any(), any(), any())).thenReturn(Collections.emptyList());
        when(customerRepository.findByPhone("0912345678")).thenReturn(Optional.of(Customer.builder().build()));

        // Giả lập 2 transaction cùng commit và DB Unique Index chặn transaction thứ hai
        when(bookingRepository.save(any(Booking.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation: uq_booking_items_active_trip_seat"));

        assertThatThrownBy(() -> transactionService.holdSeats(request, staffUser))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_ALREADY_RESERVED);
    }

    @Test
    @DisplayName("Xác nhận đơn thành công: Cập nhật CONFIRMED, lưu Payment và phát hành Ticket")
    void confirmBooking_Success() {
        UUID bookingId = UUID.randomUUID();
        Customer customer = Customer.builder().id(UUID.randomUUID()).phone("0912345678").fullName("Khách").build();

        BookingItem item1 = BookingItem.builder()
                .id(UUID.randomUUID())
                .seat(seatA01)
                .seatCode("A01")
                .basePrice(new BigDecimal("250000.00"))
                .seatExtraPrice(new BigDecimal("50000.00"))
                .finalPrice(new BigDecimal("300000.00"))
                .status(BookingItemStatus.HELD)
                .build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingCode("DL-261011-0001")
                .trip(trip)
                .customer(customer)
                .status(BookingStatus.HELD)
                .holdExpiresAt(OffsetDateTime.now().plusMinutes(5)) // Còn hạn
                .createdBy("staff_hoa")
                .items(List.of(item1))
                .build();
        item1.setBooking(booking);

        ConfirmBookingRequest request = ConfirmBookingRequest.builder()
                .customerName("Khách VIP")
                .customerPhone("0912345678")
                .passengers(List.of(PassengerItemRequest.builder()
                        .seatId(seatA01.getId())
                        .passengerName("Khách VIP")
                        .passengerPhone("0912345678")
                        .build()))
                .payment(PaymentRequest.builder()
                        .method(PaymentMethod.CASH)
                        .amount(new BigDecimal("300000.00")) // Đúng tổng tiền
                        .build())
                .build();

        when(bookingRepository.findByIdWithDetails(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        BookingResponse response = transactionService.confirmBooking(bookingId, request, staffUser);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(paymentRepository).save(any());
        verify(ticketRepository).save(any());
    }

    @Test
    @DisplayName("Từ chối xác nhận đơn khi đã hết thời hạn giữ chỗ 10 phút (SEAT_HOLD_EXPIRED)")
    void confirmBooking_ThrowsException_WhenHoldExpired() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.HELD)
                .holdExpiresAt(OffsetDateTime.now().minusMinutes(1)) // Đã quá hạn
                .createdBy("staff_hoa")
                .items(List.of(BookingItem.builder().status(BookingItemStatus.HELD).build()))
                .build();

        when(bookingRepository.findByIdWithDetails(bookingId)).thenReturn(Optional.of(booking));

        ConfirmBookingRequest request = ConfirmBookingRequest.builder()
                .customerName("Khách")
                .customerPhone("0912345678")
                .passengers(List.of())
                .payment(PaymentRequest.builder().amount(BigDecimal.ZERO).method(PaymentMethod.CASH).build())
                .build();

        assertThatThrownBy(() -> transactionService.confirmBooking(bookingId, request, staffUser))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_HOLD_EXPIRED);
    }

    @Test
    @DisplayName("Kiểm tra quyền sở hữu (Ownership): Chặn nhân viên khác xác nhận đơn không phải do mình tạo")
    void confirmBooking_ThrowsException_WhenNonOwnerAttemptsConfirmation() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.HELD)
                .holdExpiresAt(OffsetDateTime.now().plusMinutes(5))
                .createdBy("staff_hoa") // Do staff_hoa tạo
                .build();

        when(bookingRepository.findByIdWithDetails(bookingId)).thenReturn(Optional.of(booking));

        ConfirmBookingRequest request = ConfirmBookingRequest.builder()
                .customerName("Khách")
                .customerPhone("0912345678")
                .passengers(List.of())
                .payment(PaymentRequest.builder().amount(BigDecimal.ZERO).method(PaymentMethod.CASH).build())
                .build();

        // staff_nam cố tình xác nhận
        assertThatThrownBy(() -> transactionService.confirmBooking(bookingId, request, otherStaffUser))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("Kiểm tra giá tại Backend: Ném lỗi khi số tiền thanh toán không khớp với giá tính toán")
    void confirmBooking_ThrowsException_WhenPaymentAmountMismatches() {
        UUID bookingId = UUID.randomUUID();
        Customer customer = Customer.builder().id(UUID.randomUUID()).phone("0912345678").fullName("Khách").build();

        BookingItem item1 = BookingItem.builder()
                .id(UUID.randomUUID())
                .seat(seatA01)
                .seatCode("A01")
                .basePrice(new BigDecimal("250000.00"))
                .seatExtraPrice(new BigDecimal("50000.00"))
                .finalPrice(new BigDecimal("300000.00"))
                .status(BookingItemStatus.HELD)
                .build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .trip(trip)
                .customer(customer)
                .status(BookingStatus.HELD)
                .holdExpiresAt(OffsetDateTime.now().plusMinutes(5))
                .createdBy("staff_hoa")
                .items(List.of(item1))
                .build();

        ConfirmBookingRequest request = ConfirmBookingRequest.builder()
                .customerName("Khách")
                .customerPhone("0912345678")
                .passengers(List.of(PassengerItemRequest.builder()
                        .seatId(seatA01.getId())
                        .passengerName("Khách")
                        .build()))
                .payment(PaymentRequest.builder()
                        .method(PaymentMethod.CASH)
                        .amount(new BigDecimal("200000.00")) // Truyền sai số tiền (Client gửi 200k thay vì 300k)
                        .build())
                .build();

        when(bookingRepository.findByIdWithDetails(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> transactionService.confirmBooking(bookingId, request, staffUser))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không khớp với tổng tiền thực tế của đơn vé");
    }

    @Test
    @DisplayName("Hủy đơn đặt vé thành công: chuyển trạng thái CANCELLED và giải phóng ghế")
    void cancelBooking_Success() {
        UUID bookingId = UUID.randomUUID();
        BookingItem item1 = BookingItem.builder()
                .id(UUID.randomUUID())
                .seat(seatA01)
                .seatCode("A01")
                .status(BookingItemStatus.CONFIRMED)
                .build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingCode("DL-261011-0001")
                .trip(trip)
                .status(BookingStatus.CONFIRMED)
                .items(List.of(item1))
                .build();

        when(bookingRepository.findByIdWithDetails(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        BookingResponse response = transactionService.cancelBooking(bookingId, "Khách bận việc đột xuất", staffUser);

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getItems().get(0).getStatus()).isEqualTo(BookingItemStatus.CANCELLED);
        assertThat(booking.getNote()).contains("Lý do hủy: Khách bận việc đột xuất");
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("Chặn hủy đơn khi đơn đã hoàn thành hoặc đã bị hủy trước đó")
    void cancelBooking_ThrowsException_WhenInvalidStatus() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingCode("DL-261011-0001")
                .status(BookingStatus.COMPLETED)
                .build();

        when(bookingRepository.findByIdWithDetails(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> transactionService.cancelBooking(bookingId, "Hủy thử", staffUser))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không thể hủy đơn đặt vé");
    }
}
