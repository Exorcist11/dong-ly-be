package com.dongly.modules.booking;

import com.dongly.common.exception.ResourceNotFoundException;
import com.dongly.modules.booking.dto.CrmTripSearchCriteria;
import com.dongly.modules.booking.dto.CrmTripSearchResultResponse;
import com.dongly.modules.booking.dto.CrmTripSeatMapResponse;
import com.dongly.modules.booking.entity.SeatOccupancyStatus;
import com.dongly.modules.booking.service.CrmBookingQueryService;
import com.dongly.modules.fleet.entity.SeatStatus;
import com.dongly.modules.fleet.entity.SeatType;
import com.dongly.modules.fleet.entity.Vehicle;
import com.dongly.modules.fleet.entity.VehicleSeat;
import com.dongly.modules.fleet.entity.VehicleStatus;
import com.dongly.modules.fleet.entity.VehicleType;
import com.dongly.modules.fleet.repository.VehicleSeatRepository;
import com.dongly.modules.route.entity.CommonStatus;
import com.dongly.modules.route.entity.Location;
import com.dongly.modules.route.entity.Route;
import com.dongly.modules.route.entity.RouteDirectionType;
import com.dongly.modules.route.entity.RouteStop;
import com.dongly.modules.route.entity.RouteStopType;
import com.dongly.modules.route.entity.StopPoint;
import com.dongly.modules.route.repository.RouteStopRepository;
import com.dongly.modules.trip.entity.Trip;
import com.dongly.modules.trip.entity.TripStatus;
import com.dongly.modules.trip.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmBookingQueryServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private VehicleSeatRepository vehicleSeatRepository;

    @Mock
    private RouteStopRepository routeStopRepository;

    @Mock
    private com.dongly.modules.booking.repository.BookingItemRepository bookingItemRepository;

    @Mock
    private com.dongly.modules.booking.repository.BookingRepository bookingRepository;

    @InjectMocks
    private CrmBookingQueryService crmBookingQueryService;

    private Trip sampleTrip;
    private Vehicle sampleVehicle;
    private Route sampleRoute;
    private Location locThanhHoa;
    private Location locHaNoi;

    @BeforeEach
    void setUp() {
        locThanhHoa = Location.builder()
                .id(UUID.randomUUID())
                .code("THANH_HOA")
                .name("Thanh Hóa")
                .status(CommonStatus.ACTIVE)
                .build();

        locHaNoi = Location.builder()
                .id(UUID.randomUUID())
                .code("HA_NOI")
                .name("Hà Nội")
                .status(CommonStatus.ACTIVE)
                .build();

        sampleRoute = Route.builder()
                .id(UUID.randomUUID())
                .code("TH_HN")
                .name("Thanh Hóa - Hà Nội")
                .originLocation(locThanhHoa)
                .destinationLocation(locHaNoi)
                .status(CommonStatus.ACTIVE)
                .distanceKm(new BigDecimal("160.00"))
                .estimatedDurationMinutes(180)
                .build();

        sampleVehicle = Vehicle.builder()
                .id(UUID.randomUUID())
                .plateNumber("36B-028.68")
                .vehicleType(VehicleType.LIMOUSINE)
                .status(VehicleStatus.ACTIVE)
                .totalFloors(2)
                .totalRows(6)
                .totalColumns(3)
                .totalSeats(22)
                .build();

        sampleTrip = Trip.builder()
                .id(UUID.randomUUID())
                .code("TRP-20261015-0400-36B02868")
                .route(sampleRoute)
                .vehicle(sampleVehicle)
                .departureTime(OffsetDateTime.of(2026, 10, 15, 4, 0, 0, 0, ZoneOffset.ofHours(7)))
                .estimatedArrivalTime(OffsetDateTime.of(2026, 10, 15, 7, 0, 0, 0, ZoneOffset.ofHours(7)))
                .basePrice(new BigDecimal("250000.00"))
                .status(TripStatus.READY)
                .build();
    }

    @Test
    @DisplayName("Tìm kiếm chuyến xe khả dụng thành công theo ngày và tuyến đường")
    void searchTrips_Success() {
        CrmTripSearchCriteria criteria = CrmTripSearchCriteria.builder()
                .routeId(sampleRoute.getId())
                .departureDate(LocalDate.of(2026, 10, 15))
                .page(0)
                .size(20)
                .build();

        Page<Trip> pageTrips = new PageImpl<>(List.of(sampleTrip));
        when(tripRepository.searchCrmTrips(
                eq(sampleRoute.getId()), any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(pageTrips);

        Page<CrmTripSearchResultResponse> results = crmBookingQueryService.searchTrips(criteria);

        assertThat(results).isNotNull();
        assertThat(results.getContent()).hasSize(1);
        CrmTripSearchResultResponse res = results.getContent().get(0);
        assertThat(res.getTripCode()).isEqualTo("TRP-20261015-0400-36B02868");
        assertThat(res.getRouteName()).isEqualTo("Thanh Hóa - Hà Nội");
        assertThat(res.getVehiclePlateNumber()).isEqualTo("36B-028.68");
        assertThat(res.getTotalSeats()).isEqualTo(22);
        assertThat(res.getAvailableSeats()).isEqualTo(22);
        assertThat(res.getBasePrice()).isEqualByComparingTo("250000.00");
    }

    @Test
    @DisplayName("Lấy sơ đồ ghế chuyến xe thành công và phân biệt ghế AVAILABLE vs LOCKED")
    void getTripSeatMap_Success_DifferentiatesAvailableAndLocked() {
        UUID tripId = sampleTrip.getId();

        VehicleSeat seatA01 = VehicleSeat.builder()
                .id(UUID.randomUUID())
                .vehicle(sampleVehicle)
                .seatCode("A01")
                .floor(1)
                .rowIndex(1)
                .columnIndex(1)
                .seatType(SeatType.LUXURY_ROOM)
                .extraPrice(new BigDecimal("50000.00"))
                .status(SeatStatus.ACTIVE)
                .build();

        VehicleSeat seatB01 = VehicleSeat.builder()
                .id(UUID.randomUUID())
                .vehicle(sampleVehicle)
                .seatCode("B01")
                .floor(2)
                .rowIndex(1)
                .columnIndex(1)
                .seatType(SeatType.LUXURY_ROOM)
                .extraPrice(new BigDecimal("30000.00"))
                .status(SeatStatus.BLOCKED) // Ghế bảo trì / tạm khóa
                .build();

        StopPoint spBigC = StopPoint.builder()
                .id(UUID.randomUUID())
                .name("Big C Thanh Hóa")
                .address("Đại lộ Hùng Vương, TP Thanh Hóa")
                .build();

        RouteStop stop1 = RouteStop.builder()
                .id(UUID.randomUUID())
                .route(sampleRoute)
                .stopPoint(spBigC)
                .direction(RouteDirectionType.OUTBOUND)
                .sequence(1)
                .stopType(RouteStopType.PICKUP)
                .extraPrice(BigDecimal.ZERO)
                .status(CommonStatus.ACTIVE)
                .build();

        when(tripRepository.findByIdWithDetails(tripId)).thenReturn(Optional.of(sampleTrip));
        when(vehicleSeatRepository.findByVehicleIdOrderByFloorAscRowIndexAscColumnIndexAsc(sampleVehicle.getId()))
                .thenReturn(List.of(seatA01, seatB01));
        when(routeStopRepository.findByRouteIdOrderBySequenceAsc(sampleRoute.getId()))
                .thenReturn(List.of(stop1));

        CrmTripSeatMapResponse response = crmBookingQueryService.getTripSeatMap(tripId);

        assertThat(response).isNotNull();
        assertThat(response.getTripCode()).isEqualTo(sampleTrip.getCode());
        assertThat(response.getSeats()).hasSize(2);
        assertThat(response.getStops()).hasSize(1);

        // Ghế A01: ACTIVE -> AVAILABLE, isBookable = true, calculatedPrice = 250k + 50k = 300k
        var seatA01Dto = response.getSeats().get(0);
        assertThat(seatA01Dto.getSeatCode()).isEqualTo("A01");
        assertThat(seatA01Dto.getOccupancyStatus()).isEqualTo(SeatOccupancyStatus.AVAILABLE);
        assertThat(seatA01Dto.isBookable()).isTrue();
        assertThat(seatA01Dto.getCalculatedPrice()).isEqualByComparingTo("300000.00");

        // Ghế B01: BLOCKED -> LOCKED, isBookable = false
        var seatB01Dto = response.getSeats().get(1);
        assertThat(seatB01Dto.getSeatCode()).isEqualTo("B01");
        assertThat(seatB01Dto.getOccupancyStatus()).isEqualTo(SeatOccupancyStatus.LOCKED);
        assertThat(seatB01Dto.isBookable()).isFalse();

        assertThat(response.getAvailableSeats()).isEqualTo(1);
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi không tìm thấy chuyến xe")
    void getTripSeatMap_ThrowsException_WhenTripNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(tripRepository.findByIdWithDetails(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> crmBookingQueryService.getTripSeatMap(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
