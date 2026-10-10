package com.dongly.modules.trip;

import com.dongly.common.exception.BusinessRuleException;
import com.dongly.modules.fleet.entity.Driver;
import com.dongly.modules.fleet.entity.Vehicle;
import com.dongly.modules.route.entity.Route;
import com.dongly.modules.trip.dto.GenerateTripsPreviewResponse;
import com.dongly.modules.trip.dto.GenerateTripsRequest;
import com.dongly.modules.trip.dto.GenerateTripsResultResponse;
import com.dongly.modules.trip.entity.Trip;
import com.dongly.modules.trip.entity.TripRun;
import com.dongly.modules.trip.entity.TripRunStatus;
import com.dongly.modules.trip.repository.TripRepository;
import com.dongly.modules.trip.repository.TripRunRepository;
import com.dongly.modules.trip.service.TripGeneratorService;
import com.dongly.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripGeneratorServiceTest {

    @Mock
    private TripRunRepository tripRunRepository;

    @Mock
    private TripRepository tripRepository;

    @InjectMocks
    private TripGeneratorService tripGeneratorService;

    private TripRun tripRun;
    private CurrentUser currentUser;

    @BeforeEach
    void setUp() {
        Route route = Route.builder()
                .id(UUID.randomUUID())
                .code("ROUTE-HN-TS")
                .name("Hà Nội - Triệu Sơn")
                .estimatedDurationMinutes(180)
                .build();

        Vehicle vehicle = Vehicle.builder()
                .id(UUID.randomUUID())
                .plateNumber("36B-028.68")
                .build();

        Driver driver = Driver.builder()
                .id(UUID.randomUUID())
                .fullName("Nguyễn Văn A")
                .build();

        Driver assistant = Driver.builder()
                .id(UUID.randomUUID())
                .fullName("Lê Văn B")
                .build();

        tripRun = TripRun.builder()
                .id(UUID.randomUUID())
                .code("TR-HN-TS-01")
                .name("Vòng chạy sáng HN - TS")
                .route(route)
                .departureTime(LocalTime.of(7, 0))
                .daysOfWeek("1,2,3,4,5,6,7") // Chạy tất cả các ngày
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusMonths(6))
                .defaultVehicle(vehicle)
                .defaultDriver(driver)
                .defaultAssistantDriver(assistant)
                .basePrice(new BigDecimal("250000.00"))
                .status(TripRunStatus.ACTIVE)
                .build();

        currentUser = new CurrentUser(UUID.randomUUID(), "admin", "admin@dongly.vn", Set.of("ADMIN"), Set.of("TRIP_MANAGE"));
    }

    @Test
    @DisplayName("Xem trước danh sách chuyến dự kiến sinh cho khoảng 7 ngày")
    void previewGenerateTrips_Success() {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(6); // 7 ngày

        GenerateTripsRequest request = GenerateTripsRequest.builder()
                .tripRunId(tripRun.getId())
                .fromDate(from)
                .toDate(to)
                .build();

        when(tripRunRepository.findById(tripRun.getId())).thenReturn(Optional.of(tripRun));
        when(tripRepository.existsByTripRunIdAndDepartureTime(any(), any())).thenReturn(false);

        GenerateTripsPreviewResponse preview = tripGeneratorService.previewGenerateTrips(request);

        assertThat(preview.getTotalDatesChecked()).isEqualTo(7);
        assertThat(preview.getWillCreateCount()).isEqualTo(7);
        assertThat(preview.getAlreadyExistsCount()).isEqualTo(0);
        assertThat(preview.getItems()).hasSize(7);
    }

    @Test
    @DisplayName("Chống sinh trùng chuyến: Bỏ qua (skip) ngày đã tồn tại chuyến")
    void executeGenerateTrips_Idempotent_SkipsExistingTrips() {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(2); // 3 ngày: Day 0, Day 1, Day 2

        GenerateTripsRequest request = GenerateTripsRequest.builder()
                .tripRunId(tripRun.getId())
                .fromDate(from)
                .toDate(to)
                .build();

        when(tripRunRepository.findById(tripRun.getId())).thenReturn(Optional.of(tripRun));
        // Giả sử ngày đầu tiên đã tồn tại chuyến, 2 ngày sau chưa có
        when(tripRepository.existsByTripRunIdAndDepartureTime(any(), any()))
                .thenReturn(true)  // Day 0: đã có
                .thenReturn(false) // Day 1: chưa có
                .thenReturn(false); // Day 2: chưa có

        when(tripRepository.save(any(Trip.class))).thenAnswer(i -> {
            Trip t = i.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        GenerateTripsResultResponse result = tripGeneratorService.executeGenerateTrips(request, currentUser);

        assertThat(result.getTotalDatesChecked()).isEqualTo(3);
        assertThat(result.getCreatedCount()).isEqualTo(2);
        assertThat(result.getSkippedCount()).isEqualTo(1);
        verify(tripRepository, times(2)).save(any(Trip.class));
    }

    @Test
    @DisplayName("Từ chối sinh chuyến nếu khoảng ngày yêu cầu vượt quá giới hạn 60 ngày")
    void executeGenerateTrips_ThrowsException_WhenDateRangeExceedsMax() {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(65); // 66 ngày > 60 ngày

        GenerateTripsRequest request = GenerateTripsRequest.builder()
                .tripRunId(tripRun.getId())
                .fromDate(from)
                .toDate(to)
                .build();

        assertThatThrownBy(() -> tripGeneratorService.executeGenerateTrips(request, currentUser))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Khoảng thời gian sinh chuyến tối đa là 60 ngày");
    }
}
