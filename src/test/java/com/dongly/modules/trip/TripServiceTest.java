package com.dongly.modules.trip;

import com.dongly.common.exception.BusinessRuleException;
import com.dongly.modules.fleet.entity.Driver;
import com.dongly.modules.fleet.entity.DriverStatus;
import com.dongly.modules.fleet.entity.Vehicle;
import com.dongly.modules.fleet.entity.VehicleStatus;
import com.dongly.modules.fleet.entity.VehicleType;
import com.dongly.modules.fleet.repository.DriverRepository;
import com.dongly.modules.fleet.repository.VehicleRepository;
import com.dongly.modules.route.entity.CommonStatus;
import com.dongly.modules.route.entity.Route;
import com.dongly.modules.route.repository.RouteRepository;
import com.dongly.modules.trip.dto.ConflictCheckRequest;
import com.dongly.modules.trip.dto.ConflictCheckResponse;
import com.dongly.modules.trip.dto.CreateTripRequest;
import com.dongly.modules.trip.dto.TripResponse;
import com.dongly.modules.trip.dto.UpdateTripStatusRequest;
import com.dongly.modules.trip.entity.Trip;
import com.dongly.modules.trip.entity.TripStatus;
import com.dongly.modules.trip.repository.TripRepository;
import com.dongly.modules.trip.repository.TripRunRepository;
import com.dongly.modules.trip.service.TripService;
import com.dongly.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripRunRepository tripRunRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private TripService tripService;

    private Route sampleRoute;
    private Vehicle sampleVehicle;
    private Driver primaryDriver;
    private Driver assistantDriver;
    private CurrentUser currentUser;
    private OffsetDateTime departureTime;
    private OffsetDateTime arrivalTime;

    @BeforeEach
    void setUp() {
        UUID routeId = UUID.randomUUID();
        sampleRoute = Route.builder()
                .id(routeId)
                .code("ROUTE-HN-TS")
                .name("Hà Nội - Triệu Sơn")
                .status(CommonStatus.ACTIVE)
                .estimatedDurationMinutes(180)
                .build();

        sampleVehicle = Vehicle.builder()
                .id(UUID.randomUUID())
                .plateNumber("36B-028.68")
                .vehicleType(VehicleType.LIMOUSINE)
                .status(VehicleStatus.ACTIVE)
                .totalSeats(22)
                .build();

        primaryDriver = Driver.builder()
                .id(UUID.randomUUID())
                .code("TX-001")
                .fullName("Nguyễn Văn A")
                .status(DriverStatus.ACTIVE)
                .phone("0912345678")
                .build();

        assistantDriver = Driver.builder()
                .id(UUID.randomUUID())
                .code("TX-002")
                .fullName("Lê Văn B")
                .status(DriverStatus.ACTIVE)
                .phone("0987654321")
                .build();

        currentUser = new CurrentUser(UUID.randomUUID(), "admin", "admin@dongly.vn", Set.of("ADMIN"), Set.of("TRIP_MANAGE"));

        departureTime = OffsetDateTime.now(ZoneOffset.ofHours(7)).plusDays(1).withHour(7).withMinute(0);
        arrivalTime = departureTime.plusMinutes(180);
    }

    @Test
    @DisplayName("Tạo chuyến thành công khi đủ phương tiện, tài xế chính và phụ xe hợp lệ")
    void createTrip_Success() {
        CreateTripRequest request = CreateTripRequest.builder()
                .routeId(sampleRoute.getId())
                .vehicleId(sampleVehicle.getId())
                .driverId(primaryDriver.getId())
                .assistantDriverId(assistantDriver.getId())
                .departureTime(departureTime)
                .estimatedArrivalTime(arrivalTime)
                .basePrice(new BigDecimal("250000.00"))
                .build();

        when(routeRepository.findById(sampleRoute.getId())).thenReturn(Optional.of(sampleRoute));
        when(vehicleRepository.findById(sampleVehicle.getId())).thenReturn(Optional.of(sampleVehicle));
        when(driverRepository.findById(primaryDriver.getId())).thenReturn(Optional.of(primaryDriver));
        when(driverRepository.findById(assistantDriver.getId())).thenReturn(Optional.of(assistantDriver));
        when(tripRepository.findActiveTripsByVehicleInWindow(any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(tripRepository.findActiveTripsByDriverInWindow(any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TripResponse response = tripService.createTrip(request, currentUser);

        assertThat(response).isNotNull();
        assertThat(response.getRouteName()).isEqualTo("Hà Nội - Triệu Sơn");
        assertThat(response.getVehiclePlateNumber()).isEqualTo("36B-028.68");
        assertThat(response.getDriverName()).isEqualTo("Nguyễn Văn A");
        assertThat(response.getAssistantDriverName()).isEqualTo("Lê Văn B");
        verify(tripRepository).save(any(Trip.class));
    }

    @Test
    @DisplayName("Ném ngoại lệ khi tài xế chính và phụ xe trùng một người")
    void createTrip_ThrowsException_WhenSameDriverAndAssistant() {
        CreateTripRequest request = CreateTripRequest.builder()
                .routeId(sampleRoute.getId())
                .vehicleId(sampleVehicle.getId())
                .driverId(primaryDriver.getId())
                .assistantDriverId(primaryDriver.getId()) // Trùng nhau
                .departureTime(departureTime)
                .basePrice(new BigDecimal("250000.00"))
                .build();

        when(routeRepository.findById(sampleRoute.getId())).thenReturn(Optional.of(sampleRoute));
        when(vehicleRepository.findById(sampleVehicle.getId())).thenReturn(Optional.of(sampleVehicle));
        when(driverRepository.findById(primaryDriver.getId())).thenReturn(Optional.of(primaryDriver));

        assertThatThrownBy(() -> tripService.createTrip(request, currentUser))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Tài xế chính và phụ xe không được là cùng một người");
    }

    @Test
    @DisplayName("Phát hiện xung đột khi phương tiện đã được phân công cho chuyến xe khác trong khoảng thời gian (kèm buffer 45p)")
    void checkConflict_DetectsVehicleConflict() {
        Trip conflictingTrip = Trip.builder()
                .id(UUID.randomUUID())
                .code("TRP-OLD-01")
                .vehicle(sampleVehicle)
                .departureTime(departureTime.minusMinutes(60))
                .estimatedArrivalTime(departureTime.plusMinutes(60))
                .status(TripStatus.READY)
                .build();

        when(tripRepository.findActiveTripsByVehicleInWindow(eq(sampleVehicle.getId()), any(), any(), any()))
                .thenReturn(List.of(conflictingTrip));

        ConflictCheckRequest checkRequest = ConflictCheckRequest.builder()
                .vehicleId(sampleVehicle.getId())
                .departureTime(departureTime)
                .estimatedArrivalTime(arrivalTime)
                .build();

        ConflictCheckResponse response = tripService.checkConflict(checkRequest);

        assertThat(response.isHasConflict()).isTrue();
        assertThat(response.isVehicleConflict()).isTrue();
        assertThat(response.getConflictMessages()).anyMatch(msg -> msg.contains("Phương tiện 36B-028.68"));
    }

    @Test
    @DisplayName("Cập nhật trạng thái chuyến xe theo State Machine hợp lệ")
    void updateTripStatus_ValidTransitions() {
        Trip trip = Trip.builder()
                .id(UUID.randomUUID())
                .code("TRP-001")
                .status(TripStatus.SCHEDULED)
                .departureTime(departureTime)
                .estimatedArrivalTime(arrivalTime)
                .build();

        when(tripRepository.findByIdWithDetails(trip.getId())).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(i -> i.getArgument(0));

        // SCHEDULED -> READY
        UpdateTripStatusRequest readyReq = UpdateTripStatusRequest.builder()
                .status(TripStatus.READY)
                .build();
        TripResponse readyRes = tripService.updateStatus(trip.getId(), readyReq, currentUser);
        assertThat(readyRes.getStatus()).isEqualTo(TripStatus.READY);

        // READY -> DEPARTED
        UpdateTripStatusRequest departedReq = UpdateTripStatusRequest.builder()
                .status(TripStatus.DEPARTED)
                .build();
        TripResponse departedRes = tripService.updateStatus(trip.getId(), departedReq, currentUser);
        assertThat(departedRes.getStatus()).isEqualTo(TripStatus.DEPARTED);

        // DEPARTED -> COMPLETED
        UpdateTripStatusRequest completedReq = UpdateTripStatusRequest.builder()
                .status(TripStatus.COMPLETED)
                .build();
        TripResponse completedRes = tripService.updateStatus(trip.getId(), completedReq, currentUser);
        assertThat(completedRes.getStatus()).isEqualTo(TripStatus.COMPLETED);
    }

    @Test
    @DisplayName("Từ chối chuyển trạng thái bất hợp lệ từ COMPLETED về DEPARTED")
    void updateTripStatus_InvalidTransition_ThrowsException() {
        Trip trip = Trip.builder()
                .id(UUID.randomUUID())
                .code("TRP-001")
                .status(TripStatus.COMPLETED)
                .build();

        when(tripRepository.findByIdWithDetails(trip.getId())).thenReturn(Optional.of(trip));

        UpdateTripStatusRequest invalidReq = UpdateTripStatusRequest.builder()
                .status(TripStatus.DEPARTED)
                .build();

        assertThatThrownBy(() -> tripService.updateStatus(trip.getId(), invalidReq, currentUser))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Không thể chuyển trạng thái chuyến xe");
    }
}
