package com.dongly.modules.booking.service;

import com.dongly.common.exception.AppException;
import com.dongly.common.exception.BusinessRuleException;
import com.dongly.common.exception.ErrorCode;
import com.dongly.common.exception.ResourceNotFoundException;
import com.dongly.modules.booking.dto.BookingResponse;
import com.dongly.modules.booking.dto.ConfirmBookingRequest;
import com.dongly.modules.booking.dto.HoldSeatsRequest;
import com.dongly.modules.booking.dto.HoldSeatsResponse;
import com.dongly.modules.booking.dto.PassengerItemRequest;
import com.dongly.modules.booking.entity.Booking;
import com.dongly.modules.booking.entity.BookingChannel;
import com.dongly.modules.booking.entity.BookingItem;
import com.dongly.modules.booking.entity.BookingItemStatus;
import com.dongly.modules.booking.entity.BookingStatus;
import com.dongly.modules.booking.entity.Customer;
import com.dongly.modules.booking.entity.Payment;
import com.dongly.modules.booking.entity.PaymentStatus;
import com.dongly.modules.booking.entity.Ticket;
import com.dongly.modules.booking.entity.TicketStatus;
import com.dongly.modules.booking.repository.BookingItemRepository;
import com.dongly.modules.booking.repository.BookingRepository;
import com.dongly.modules.booking.repository.CustomerRepository;
import com.dongly.modules.booking.repository.PaymentRepository;
import com.dongly.modules.booking.repository.TicketRepository;
import com.dongly.modules.fleet.entity.SeatStatus;
import com.dongly.modules.fleet.entity.VehicleSeat;
import com.dongly.modules.fleet.repository.VehicleSeatRepository;
import com.dongly.modules.route.entity.CommonStatus;
import com.dongly.modules.route.entity.RouteStop;
import com.dongly.modules.route.repository.RouteStopRepository;
import com.dongly.modules.trip.entity.Trip;
import com.dongly.modules.trip.entity.TripStatus;
import com.dongly.modules.trip.repository.TripRepository;
import com.dongly.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service xử lý các nghiệp vụ Giao dịch Booking:
 * - Giữ ghế tạm thời (Hold 10 phút, Pessimistic lock, Partial Unique Index DB).
 * - Xác nhận đơn đặt vé, tính giá tại Backend, lưu Payment và phát hành Ticket.
 * - Giải phóng ghế hết hạn.
 */
@Slf4j
@Service
public class CrmBookingTransactionService {

    public static final int HOLD_DURATION_MINUTES = 10;

    private final TripRepository tripRepository;
    private final VehicleSeatRepository vehicleSeatRepository;
    private final RouteStopRepository routeStopRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    public CrmBookingTransactionService(
            TripRepository tripRepository,
            VehicleSeatRepository vehicleSeatRepository,
            RouteStopRepository routeStopRepository,
            CustomerRepository customerRepository,
            BookingRepository bookingRepository,
            BookingItemRepository bookingItemRepository,
            PaymentRepository paymentRepository,
            TicketRepository ticketRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            org.springframework.transaction.PlatformTransactionManager transactionManager
    ) {
        this.tripRepository = tripRepository;
        this.vehicleSeatRepository = vehicleSeatRepository;
        this.routeStopRepository = routeStopRepository;
        this.customerRepository = customerRepository;
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.paymentRepository = paymentRepository;
        this.ticketRepository = ticketRepository;
        if (transactionManager != null) {
            org.springframework.transaction.support.TransactionTemplate tt =
                    new org.springframework.transaction.support.TransactionTemplate(transactionManager);
            tt.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            this.transactionTemplate = tt;
        } else {
            this.transactionTemplate = null;
        }
    }

    /**
     * Giữ ghế tạm thời (Hold seats) cho nhân viên CRM.
     * Áp dụng:
     * 1. Pessimistic Write Lock trên bản ghi chuyến xe (Trip) để tuần tự hóa các luồng tranh chấp ghế.
     * 2. Kiểm tra xung đột ghế và hạn giữ ghế thời gian thực (không phụ thuộc hoàn toàn vào background scheduler).
     * 3. Database Partial Unique Constraint (uq_booking_items_active_trip_seat) chặn vật lý race condition.
     */
    @Transactional
    public HoldSeatsResponse holdSeats(HoldSeatsRequest request, CurrentUser currentUser) {
        UUID tripId = request.getTripId();
        List<UUID> requestedSeatIds = request.getSeatIds();

        if (requestedSeatIds == null || requestedSeatIds.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, "Danh sách ghế cần giữ không được để trống");
        }

        // Chặn chọn trùng cùng 1 ghế trong request
        Set<UUID> uniqueSeatIds = new HashSet<>(requestedSeatIds);
        if (uniqueSeatIds.size() != requestedSeatIds.size()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, "Danh sách ghế có phần tử trùng lặp trong request");
        }

        // 1. Khóa bi quan (Pessimistic Lock) trên bản ghi Trip
        Trip trip = tripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("chuyến xe", tripId));

        if (trip.getStatus() != TripStatus.SCHEDULED && trip.getStatus() != TripStatus.READY) {
            throw new BusinessRuleException("Chuyến xe không ở trạng thái mở bán (SCHEDULED/READY)");
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (trip.getDepartureTime().isBefore(now)) {
            throw new BusinessRuleException("Chuyến xe đã quá giờ khởi hành, không thể giữ ghế");
        }

        // 2. Xác minh các ghế thuộc đúng phương tiện của chuyến xe và có trạng thái vật lý ACTIVE
        List<VehicleSeat> physicalSeats = vehicleSeatRepository.findAllById(requestedSeatIds);
        if (physicalSeats.size() != requestedSeatIds.size()) {
            throw new ResourceNotFoundException("Một hoặc nhiều ghế không tồn tại trong hệ thống");
        }

        UUID vehicleId = trip.getVehicle().getId();
        for (VehicleSeat seat : physicalSeats) {
            if (!seat.getVehicle().getId().equals(vehicleId)) {
                throw new BusinessRuleException(
                        "Ghế '" + seat.getSeatCode() + "' không thuộc phương tiện của chuyến xe này"
                );
            }
            if (seat.getStatus() != SeatStatus.ACTIVE) {
                throw new AppException(
                        ErrorCode.SEAT_NOT_AVAILABLE,
                        "Ghế '" + seat.getSeatCode() + "' đang tạm khóa hoặc không mở bán"
                );
            }
        }

        // 3. Kiểm tra xem có ghế nào đang bị chiếm chỗ (CONFIRMED hoặc HELD còn hạn)
        List<BookingItem> conflictingItems = bookingItemRepository.findConflictingSeats(tripId, requestedSeatIds, now);
        if (!conflictingItems.isEmpty()) {
            String conflictCodes = conflictingItems.stream()
                    .map(BookingItem::getSeatCode)
                    .distinct()
                    .collect(Collectors.joining(", "));
            throw new AppException(
                    ErrorCode.SEAT_ALREADY_RESERVED,
                    "Ghế sau đã có người giữ hoặc đặt chỗ: " + conflictCodes
            );
        }

        // 4. Tìm hoặc khởi tạo Customer (CRM / Khách đặt)
        String phone = request.getCustomerPhone().trim();
        String customerName = request.getCustomerName().trim();
        Customer customer = customerRepository.findByPhone(phone)
                .orElseGet(() -> customerRepository.save(
                        Customer.builder()
                                .phone(phone)
                                .fullName(customerName)
                                .createdBy(currentUser != null ? currentUser.username() : "SYSTEM")
                                .updatedBy(currentUser != null ? currentUser.username() : "SYSTEM")
                                .build()
                ));

        // 5. Khởi tạo Booking Header
        String staffUsername = currentUser != null ? currentUser.username() : "SYSTEM";
        OffsetDateTime expiresAt = now.plusMinutes(HOLD_DURATION_MINUTES);
        String bookingCode = generateBookingCode(now);

        BigDecimal basePrice = trip.getBasePrice() != null ? trip.getBasePrice() : BigDecimal.ZERO;
        BigDecimal totalEstimatedAmount = BigDecimal.ZERO;

        Booking booking = Booking.builder()
                .bookingCode(bookingCode)
                .trip(trip)
                .customer(customer)
                .channel(BookingChannel.CRM_PHONE)
                .status(BookingStatus.HELD)
                .holdExpiresAt(expiresAt)
                .note(request.getNote())
                .createdBy(staffUsername)
                .updatedBy(staffUsername)
                .build();

        // 6. Khởi tạo danh sách BookingItem
        List<BookingItem> items = new ArrayList<>();
        List<String> heldSeatCodes = new ArrayList<>();

        for (VehicleSeat seat : physicalSeats) {
            BigDecimal seatExtra = seat.getExtraPrice() != null ? seat.getExtraPrice() : BigDecimal.ZERO;
            BigDecimal finalPrice = basePrice.add(seatExtra);
            totalEstimatedAmount = totalEstimatedAmount.add(finalPrice);

            BookingItem item = BookingItem.builder()
                    .booking(booking)
                    .trip(trip)
                    .seat(seat)
                    .seatCode(seat.getSeatCode())
                    .passengerName(customerName)
                    .passengerPhone(phone)
                    .basePrice(basePrice)
                    .seatExtraPrice(seatExtra)
                    .pickupExtraPrice(BigDecimal.ZERO)
                    .dropoffExtraPrice(BigDecimal.ZERO)
                    .finalPrice(finalPrice)
                    .status(BookingItemStatus.HELD)
                    .build();

            items.add(item);
            heldSeatCodes.add(seat.getSeatCode());
        }

        booking.setTotalAmount(totalEstimatedAmount);
        booking.setItems(items);

        try {
            Booking savedBooking = bookingRepository.save(booking);
            log.info("CRM giữ ghế thành công: bookingCode={}, tripCode={}, seats={}, staff={}",
                    savedBooking.getBookingCode(), trip.getCode(), heldSeatCodes, staffUsername);

            return HoldSeatsResponse.builder()
                    .bookingId(savedBooking.getId())
                    .bookingCode(savedBooking.getBookingCode())
                    .tripId(tripId)
                    .status(savedBooking.getStatus())
                    .holdExpiresAt(savedBooking.getHoldExpiresAt())
                    .heldSeats(heldSeatCodes)
                    .totalEstimatedAmount(savedBooking.getTotalAmount())
                    .customerName(customer.getFullName())
                    .customerPhone(customer.getPhone())
                    .build();
        } catch (DataIntegrityViolationException e) {
            log.warn("Bị xung đột đặt trùng ghế tại tầng Database Unique Index: {}", e.getMessage());
            throw new AppException(
                    ErrorCode.SEAT_ALREADY_RESERVED,
                    "Ghế vừa được giữ bởi nhân viên khác tại cùng thời điểm. Vui lòng chọn ghế khác."
            );
        }
    }

    /**
     * Xác nhận đơn đặt vé và xuất vé (Confirm booking):
     * 1. Kiểm tra trạng thái HELD và hạn 10 phút.
     * 2. Kiểm tra quyền sở hữu (ownership) của nhân viên tạo đơn.
     * 3. Tính toán lại toàn bộ giá vé tại Backend từ basePrice, seatExtraPrice, pickupExtraPrice, dropoffExtraPrice.
     * 4. Xác thực số tiền thanh toán từ PaymentRequest phải khớp chính xác với totalAmount (không tin tưởng Client).
     * 5. Lưu Payment, phát hành Ticket kèm mã QR code.
     */
    @Transactional
    public BookingResponse confirmBooking(UUID bookingId, ConfirmBookingRequest request, CurrentUser currentUser) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("đơn đặt chỗ", bookingId));

        // 1. Kiểm tra trạng thái đơn đặt vé
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_CONFIRMED, "Đơn đặt vé đã hoàn tất xác nhận trước đó");
        }
        if (booking.getStatus() != BookingStatus.HELD) {
            throw new AppException(
                    ErrorCode.INVALID_STATUS_TRANSITION,
                    "Đơn đặt chỗ đang ở trạng thái " + booking.getStatus() + ", không thể xác nhận"
            );
        }

        // 2. Kiểm tra thời hạn giữ chỗ 10 phút (tại Backend)
        OffsetDateTime now = OffsetDateTime.now();
        if (booking.getHoldExpiresAt().isBefore(now)) {
            if (transactionTemplate != null) {
                transactionTemplate.execute(status -> {
                    Booking b = bookingRepository.findByIdWithDetails(bookingId).orElse(null);
                    if (b != null) {
                        b.setStatus(BookingStatus.EXPIRED);
                        for (BookingItem item : b.getItems()) {
                            item.setStatus(BookingItemStatus.EXPIRED);
                        }
                        bookingRepository.save(b);
                    }
                    return null;
                });
            } else {
                booking.setStatus(BookingStatus.EXPIRED);
                for (BookingItem item : booking.getItems()) {
                    item.setStatus(BookingItemStatus.EXPIRED);
                }
                bookingRepository.save(booking);
            }
            throw new AppException(ErrorCode.SEAT_HOLD_EXPIRED, "Thời hạn giữ ghế 10 phút đã kết thúc, đơn đặt chỗ đã hết hiệu lực");
        }

        // 3. Kiểm tra quyền sở hữu (Ownership): chỉ nhân viên tạo hoặc ADMIN mới được xác nhận
        String staffUsername = currentUser != null ? currentUser.username() : "SYSTEM";
        boolean isAdmin = currentUser != null && currentUser.roles() != null && currentUser.roles().contains("ADMIN");
        if (!isAdmin && booking.getCreatedBy() != null && !booking.getCreatedBy().equals(staffUsername)) {
            throw new AppException(
                    ErrorCode.ACCESS_DENIED,
                    "Đơn giữ chỗ này được tạo bởi nhân viên '" + booking.getCreatedBy() + "', bạn không có quyền xác nhận"
            );
        }

        // 4. Cập nhật thông tin khách hàng nếu có thay đổi
        Customer customer = booking.getCustomer();
        customer.setFullName(request.getCustomerName().trim());
        if (request.getCustomerEmail() != null && !request.getCustomerEmail().isBlank()) {
            customer.setEmail(request.getCustomerEmail().trim());
        }
        customer.setUpdatedBy(staffUsername);
        customerRepository.save(customer);

        // 5. Xác minh thông tin từng hành khách và tính toán lại giá vé tại Backend
        Map<UUID, PassengerItemRequest> passengerMap = request.getPassengers().stream()
                .collect(Collectors.toMap(PassengerItemRequest::getSeatId, Function.identity(), (a, b) -> a));

        BigDecimal calculatedTotal = BigDecimal.ZERO;
        Trip trip = booking.getTrip();
        BigDecimal basePrice = trip.getBasePrice() != null ? trip.getBasePrice() : BigDecimal.ZERO;

        for (BookingItem item : booking.getItems()) {
            PassengerItemRequest pReq = passengerMap.get(item.getSeat().getId());
            if (pReq == null) {
                throw new AppException(
                        ErrorCode.INVALID_ARGUMENT,
                        "Thiếu thông tin hành khách cho ghế: " + item.getSeatCode()
                );
            }

            item.setPassengerName(pReq.getPassengerName().trim());
            item.setPassengerPhone(pReq.getPassengerPhone() != null ? pReq.getPassengerPhone().trim() : customer.getPhone());

            // Điểm đón
            BigDecimal pickupExtra = BigDecimal.ZERO;
            if (pReq.getPickupStopId() != null) {
                RouteStop pickupStop = routeStopRepository.findById(pReq.getPickupStopId())
                        .orElseThrow(() -> new ResourceNotFoundException("điểm đón", pReq.getPickupStopId()));
                item.setPickupStop(pickupStop);
                pickupExtra = pickupStop.getExtraPrice() != null ? pickupStop.getExtraPrice() : BigDecimal.ZERO;
            }
            item.setPickupExtraPrice(pickupExtra);

            // Điểm trả
            BigDecimal dropoffExtra = BigDecimal.ZERO;
            if (pReq.getDropoffStopId() != null) {
                RouteStop dropoffStop = routeStopRepository.findById(pReq.getDropoffStopId())
                        .orElseThrow(() -> new ResourceNotFoundException("điểm trả", pReq.getDropoffStopId()));
                item.setDropoffStop(dropoffStop);
                dropoffExtra = dropoffStop.getExtraPrice() != null ? dropoffStop.getExtraPrice() : BigDecimal.ZERO;
            }
            item.setDropoffExtraPrice(dropoffExtra);

            // Giá vé cuối cùng của ghế
            BigDecimal finalPrice = basePrice.add(item.getSeatExtraPrice()).add(pickupExtra).add(dropoffExtra);
            item.setFinalPrice(finalPrice);
            item.setStatus(BookingItemStatus.CONFIRMED);

            calculatedTotal = calculatedTotal.add(finalPrice);
        }

        // 6. Kiểm tra số tiền thanh toán từ Client phải khớp chính xác với calculatedTotal
        BigDecimal paymentAmount = request.getPayment().getAmount();
        if (paymentAmount.compareTo(calculatedTotal) != 0) {
            throw new BusinessRuleException(
                    String.format("Số tiền thanh toán (%s đ) không khớp với tổng tiền thực tế của đơn vé (%s đ)",
                            paymentAmount.toPlainString(), calculatedTotal.toPlainString())
            );
        }

        // 7. Cập nhật Booking sang CONFIRMED
        booking.setTotalAmount(calculatedTotal);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUpdatedBy(staffUsername);
        if (request.getNote() != null) {
            booking.setNote(request.getNote());
        }

        Booking savedBooking = bookingRepository.save(booking);

        // 8. Tạo bản ghi Payment
        Payment payment = Payment.builder()
                .booking(savedBooking)
                .paymentMethod(request.getPayment().getMethod())
                .status(PaymentStatus.PAID)
                .amount(paymentAmount)
                .transactionCode(request.getPayment().getTransactionCode())
                .paidAt(now)
                .receivedBy(staffUsername)
                .note(request.getPayment().getNote())
                .build();
        paymentRepository.save(payment);

        // 9. Phát hành vé điện tử (Ticket) kèm mã QR cho từng ghế
        for (BookingItem item : savedBooking.getItems()) {
            String ticketCode = generateTicketCode(now, item.getSeatCode());
            String qrData = String.format("DL|%s|%s|%s|%s",
                    savedBooking.getBookingCode(), ticketCode, item.getSeatCode(), trip.getCode());

            Ticket ticket = Ticket.builder()
                    .ticketCode(ticketCode)
                    .bookingItem(item)
                    .qrCodeData(qrData)
                    .status(TicketStatus.ISSUED)
                    .issuedAt(now)
                    .build();
            ticketRepository.save(ticket);
        }

        log.info("Xác nhận đơn vé thành công: bookingCode={}, totalAmount={} đ, staff={}",
                savedBooking.getBookingCode(), savedBooking.getTotalAmount(), staffUsername);

        return BookingResponse.fromEntity(savedBooking);
    }

    /**
     * Hủy giữ ghế chủ động khi khách đổi ý hoặc hủy đơn
     */
    @Transactional
    public void cancelHold(UUID bookingId, CurrentUser currentUser) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("đơn đặt chỗ", bookingId));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new BusinessRuleException("Đơn đặt vé đã xác nhận thanh toán, không thể hủy giữ theo phương thức này");
        }

        String staffUsername = currentUser != null ? currentUser.username() : "SYSTEM";
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setUpdatedBy(staffUsername);

        for (BookingItem item : booking.getItems()) {
            item.setStatus(BookingItemStatus.CANCELLED);
        }

        bookingRepository.save(booking);
        log.info("Đã hủy đơn giữ chỗ: bookingCode={}, staff={}", booking.getBookingCode(), staffUsername);
    }

    /**
     * Hủy đơn đặt vé đã xác nhận (Booking Cancellation):
     * - Chỉ áp dụng cho đơn CONFIRMED hoặc HELD.
     * - Chuyển Booking sang CANCELLED, các BookingItem sang CANCELLED (giải phóng ghế).
     * - Không tự động hoàn tiền; ghi chú lý do hủy rõ ràng.
     */
    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, String cancelReason, CurrentUser currentUser) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("đơn đặt vé", bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("Đơn đặt vé này đã bị hủy trước đó");
        }
        if (booking.getStatus() == BookingStatus.EXPIRED) {
            throw new BusinessRuleException("Đơn giữ chỗ này đã hết hạn, không cần thao tác hủy");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessRuleException("Chuyến xe đã hoàn thành, không thể hủy đơn đặt vé");
        }

        String staffUsername = currentUser != null ? currentUser.username() : "SYSTEM";
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setUpdatedBy(staffUsername);
        if (cancelReason != null && !cancelReason.isBlank()) {
            String updatedNote = booking.getNote() != null
                    ? booking.getNote() + " | Lý do hủy: " + cancelReason.trim()
                    : "Lý do hủy: " + cancelReason.trim();
            booking.setNote(updatedNote);
        }

        for (BookingItem item : booking.getItems()) {
            item.setStatus(BookingItemStatus.CANCELLED);
        }

        Booking saved = bookingRepository.save(booking);
        log.info("CRM Hủy đơn đặt vé thành công: bookingCode={}, lý do={}, staff={}",
                saved.getBookingCode(), cancelReason, staffUsername);

        return BookingResponse.fromEntity(saved);
    }

    /**
     * Tác vụ quét giải phóng các đơn giữ chỗ đã hết hạn 10 phút
     */
    @Transactional
    public int releaseExpiredHolds() {
        OffsetDateTime now = OffsetDateTime.now();
        int updatedItems = bookingItemRepository.expireOutdatedBookingItems(now);
        int updatedBookings = bookingRepository.expireOutdatedBookings(now);
        if (updatedBookings > 0) {
            log.info("Đã tự động giải phóng {} đơn giữ chỗ quá hạn và {} ghế liên quan",
                    updatedBookings, updatedItems);
        }
        return updatedBookings;
    }

    private String generateBookingCode(OffsetDateTime time) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyMMdd");
        String prefix = "DL-" + time.format(fmt) + "-";
        return prefix + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private String generateTicketCode(OffsetDateTime time, String seatCode) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyMMdd");
        String prefix = "TK-" + time.format(fmt) + "-" + seatCode + "-";
        return prefix + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
