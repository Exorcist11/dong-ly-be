package com.dongly.modules.booking.service;

import com.dongly.common.exception.ResourceNotFoundException;
import com.dongly.modules.booking.dto.CrmTripSearchCriteria;
import com.dongly.modules.booking.dto.CrmTripSearchResultResponse;
import com.dongly.modules.booking.dto.CrmTripSeatItemDto;
import com.dongly.modules.booking.dto.CrmTripSeatMapResponse;
import com.dongly.modules.booking.dto.CrmTripStopDto;
import com.dongly.modules.booking.entity.SeatOccupancyStatus;
import com.dongly.modules.fleet.entity.SeatStatus;
import com.dongly.modules.fleet.entity.Vehicle;
import com.dongly.modules.fleet.entity.VehicleSeat;
import com.dongly.modules.fleet.repository.VehicleSeatRepository;
import com.dongly.modules.route.entity.CommonStatus;
import com.dongly.modules.route.entity.RouteStop;
import com.dongly.modules.route.repository.RouteStopRepository;
import com.dongly.modules.trip.entity.Trip;
import com.dongly.modules.trip.repository.TripRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service xử lý các nghiệp vụ CRM Booking:
 * - Tìm kiếm chuyến xe mở bán theo ngày, điểm đi, điểm đến.
 * - Lấy sơ đồ ghế và tính toán trạng thái chiếm chỗ thời gian thực.
 */
@Slf4j
@Service
public class CrmBookingQueryService {

    private final TripRepository tripRepository;
    private final VehicleSeatRepository vehicleSeatRepository;
    private final RouteStopRepository routeStopRepository;
    private final com.dongly.modules.booking.repository.BookingItemRepository bookingItemRepository;

    public CrmBookingQueryService(
            TripRepository tripRepository,
            VehicleSeatRepository vehicleSeatRepository,
            RouteStopRepository routeStopRepository,
            com.dongly.modules.booking.repository.BookingItemRepository bookingItemRepository
    ) {
        this.tripRepository = tripRepository;
        this.vehicleSeatRepository = vehicleSeatRepository;
        this.routeStopRepository = routeStopRepository;
        this.bookingItemRepository = bookingItemRepository;
    }

    /**
     * Tìm kiếm danh sách chuyến xe mở bán phục vụ CRM Booking
     */
    @Transactional(readOnly = true)
    public Page<CrmTripSearchResultResponse> searchTrips(CrmTripSearchCriteria criteria) {
        LocalDate date = criteria.getDepartureDate();
        OffsetDateTime startOfDay = date.atStartOfDay().atOffset(ZoneOffset.ofHours(7));
        OffsetDateTime endOfDay = date.atTime(LocalTime.MAX).atOffset(ZoneOffset.ofHours(7));
        OffsetDateTime now = OffsetDateTime.now();

        Sort sort = Sort.by(Sort.Direction.ASC, "departureTime");
        if (criteria.getSort() != null && criteria.getSort().contains(",")) {
            String[] parts = criteria.getSort().split(",");
            Sort.Direction dir = parts.length > 1 && parts[1].equalsIgnoreCase("desc")
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            sort = Sort.by(dir, parts[0]);
        }

        Pageable pageable = PageRequest.of(Math.max(0, criteria.getPage()), criteria.getSize(), sort);

        String cleanKeyword = (criteria.getKeyword() != null && !criteria.getKeyword().isBlank())
                ? criteria.getKeyword().trim() : null;

        Page<Trip> tripsPage = tripRepository.searchCrmTrips(
                criteria.getRouteId(),
                criteria.getOriginLocationId(),
                criteria.getDestinationLocationId(),
                startOfDay,
                endOfDay,
                cleanKeyword,
                pageable
        );

        return tripsPage.map(trip -> {
            int totalSeats = (trip.getVehicle() != null && trip.getVehicle().getTotalSeats() != null)
                    ? trip.getVehicle().getTotalSeats() : 0;

            List<com.dongly.modules.booking.entity.BookingItem> activeItems =
                    bookingItemRepository.findActiveItemsByTrip(trip.getId(), now);

            int heldSeats = 0;
            int bookedSeats = 0;
            for (com.dongly.modules.booking.entity.BookingItem bi : activeItems) {
                if (bi.getStatus() == com.dongly.modules.booking.entity.BookingItemStatus.CONFIRMED) {
                    bookedSeats++;
                } else if (bi.getStatus() == com.dongly.modules.booking.entity.BookingItemStatus.HELD) {
                    heldSeats++;
                }
            }

            int availableSeats = Math.max(0, totalSeats - heldSeats - bookedSeats);

            return CrmTripSearchResultResponse.fromTripAndSeatCounts(
                    trip, totalSeats, availableSeats, heldSeats, bookedSeats
            );
        });
    }

    /**
     * Lấy sơ đồ ghế chi tiết và tính toán trạng thái chiếm chỗ thời gian thực của một Trip cụ thể
     */
    @Transactional(readOnly = true)
    public CrmTripSeatMapResponse getTripSeatMap(UUID tripId) {
        Trip trip = tripRepository.findByIdWithDetails(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("chuyến xe", tripId));

        Vehicle vehicle = trip.getVehicle();
        if (vehicle == null) {
            throw new ResourceNotFoundException("phương tiện gắn với chuyến xe", tripId);
        }

        OffsetDateTime now = OffsetDateTime.now();

        // 1. Lấy danh sách ghế vật lý của phương tiện (sắp xếp floor -> row -> column)
        List<VehicleSeat> physicalSeats = vehicleSeatRepository
                .findByVehicleIdOrderByFloorAscRowIndexAscColumnIndexAsc(vehicle.getId());

        // 2. Lấy danh sách điểm đón/trả dọc tuyến của chuyến xe
        List<RouteStop> routeStops = trip.getRoute() != null
                ? routeStopRepository.findByRouteIdOrderBySequenceAsc(trip.getRoute().getId())
                : Collections.emptyList();

        List<CrmTripStopDto> stopDtos = routeStops.stream()
                .filter(s -> s.getStatus() == CommonStatus.ACTIVE)
                .map(CrmTripStopDto::fromEntity)
                .toList();

        // 3. Lấy danh sách các booking_items đang active trên chuyến này (CONFIRMED hoặc HELD còn hạn)
        List<com.dongly.modules.booking.entity.BookingItem> activeItems =
                bookingItemRepository.findActiveItemsByTrip(tripId, now);

        Map<UUID, com.dongly.modules.booking.entity.BookingItem> activeSeatMap = activeItems.stream()
                .collect(Collectors.toMap(bi -> bi.getSeat().getId(), Function.identity(), (a, b) -> a));

        int availableCount = 0;
        int heldCount = 0;
        int bookedCount = 0;

        List<CrmTripSeatItemDto> seatDtos = new ArrayList<>(physicalSeats.size());
        BigDecimal tripBasePrice = trip.getBasePrice() != null ? trip.getBasePrice() : BigDecimal.ZERO;

        for (VehicleSeat seat : physicalSeats) {
            BigDecimal seatExtra = seat.getExtraPrice() != null ? seat.getExtraPrice() : BigDecimal.ZERO;
            BigDecimal calculatedPrice = tripBasePrice.add(seatExtra);

            SeatOccupancyStatus occupancyStatus;
            OffsetDateTime holdExpiresAt = null;

            if (seat.getStatus() != SeatStatus.ACTIVE) {
                occupancyStatus = SeatOccupancyStatus.LOCKED;
            } else if (activeSeatMap.containsKey(seat.getId())) {
                com.dongly.modules.booking.entity.BookingItem activeItem = activeSeatMap.get(seat.getId());
                if (activeItem.getStatus() == com.dongly.modules.booking.entity.BookingItemStatus.CONFIRMED) {
                    occupancyStatus = SeatOccupancyStatus.BOOKED;
                    bookedCount++;
                } else {
                    occupancyStatus = SeatOccupancyStatus.HELD;
                    heldCount++;
                    if (activeItem.getBooking() != null) {
                        holdExpiresAt = activeItem.getBooking().getHoldExpiresAt();
                    }
                }
            } else {
                occupancyStatus = SeatOccupancyStatus.AVAILABLE;
                availableCount++;
            }

            boolean isBookable = (occupancyStatus == SeatOccupancyStatus.AVAILABLE);

            seatDtos.add(CrmTripSeatItemDto.builder()
                    .seatId(seat.getId())
                    .seatCode(seat.getSeatCode())
                    .floor(seat.getFloor() != null ? seat.getFloor() : 1)
                    .rowIndex(seat.getRowIndex())
                    .columnIndex(seat.getColumnIndex())
                    .seatType(seat.getSeatType())
                    .seatExtraPrice(seatExtra)
                    .calculatedPrice(calculatedPrice)
                    .occupancyStatus(occupancyStatus)
                    .holdExpiresAt(holdExpiresAt)
                    .isBookable(isBookable)
                    .build());
        }

        return CrmTripSeatMapResponse.of(
                trip, seatDtos, stopDtos, availableCount, heldCount, bookedCount
        );
    }
}
