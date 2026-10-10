package com.dongly.modules.trip.service;

import com.dongly.common.exception.BusinessRuleException;
import com.dongly.common.exception.ResourceNotFoundException;
import com.dongly.modules.trip.dto.GenerateTripsPreviewItem;
import com.dongly.modules.trip.dto.GenerateTripsPreviewResponse;
import com.dongly.modules.trip.dto.GenerateTripsRequest;
import com.dongly.modules.trip.dto.GenerateTripsResultResponse;
import com.dongly.modules.trip.entity.Trip;
import com.dongly.modules.trip.entity.TripRun;
import com.dongly.modules.trip.entity.TripRunStatus;
import com.dongly.modules.trip.entity.TripStatus;
import com.dongly.modules.trip.repository.TripRepository;
import com.dongly.modules.trip.repository.TripRunRepository;
import com.dongly.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TripGeneratorService {

    public static final int MAX_GENERATE_DAYS = 60;
    public static final ZoneOffset VIETNAM_OFFSET = ZoneOffset.ofHours(7);

    private final TripRunRepository tripRunRepository;
    private final TripRepository tripRepository;

    public TripGeneratorService(
            TripRunRepository tripRunRepository,
            TripRepository tripRepository
    ) {
        this.tripRunRepository = tripRunRepository;
        this.tripRepository = tripRepository;
    }

    @Transactional(readOnly = true)
    public GenerateTripsPreviewResponse previewGenerateTrips(GenerateTripsRequest request) {
        validateDateRange(request.getFromDate(), request.getToDate());

        List<TripRun> runs = resolveTripRuns(request.getTripRunId());
        List<GenerateTripsPreviewItem> items = new ArrayList<>();
        int willCreateCount = 0;
        int alreadyExistsCount = 0;

        for (TripRun run : runs) {
            LocalDate current = request.getFromDate();
            Set<Integer> activeDays = parseDaysOfWeek(run.getDaysOfWeek());

            while (!current.isAfter(request.getToDate())) {
                if (isDateEligible(run, current, activeDays)) {
                    OffsetDateTime depTime = current.atTime(run.getDepartureTime()).atOffset(VIETNAM_OFFSET);
                    boolean exists = tripRepository.existsByTripRunIdAndDepartureTime(run.getId(), depTime);

                    int duration = (run.getRoute() != null && run.getRoute().getEstimatedDurationMinutes() != null)
                            ? run.getRoute().getEstimatedDurationMinutes() : 180;
                    OffsetDateTime arrTime = depTime.plusMinutes(duration);

                    if (exists) {
                        alreadyExistsCount++;
                    } else {
                        willCreateCount++;
                    }

                    items.add(GenerateTripsPreviewItem.builder()
                            .tripRunId(run.getId())
                            .tripRunCode(run.getCode())
                            .tripRunName(run.getName())
                            .targetDate(current)
                            .dayOfWeek(current.getDayOfWeek().getValue())
                            .departureTime(depTime)
                            .estimatedArrivalTime(arrTime)
                            .routeId(run.getRoute() != null ? run.getRoute().getId() : null)
                            .routeCode(run.getRoute() != null ? run.getRoute().getCode() : null)
                            .routeName(run.getRoute() != null ? run.getRoute().getName() : null)
                            .vehiclePlateNumber(run.getDefaultVehicle() != null ? run.getDefaultVehicle().getPlateNumber() : null)
                            .driverName(run.getDefaultDriver() != null ? run.getDefaultDriver().getFullName() : null)
                            .assistantDriverName(run.getDefaultAssistantDriver() != null ? run.getDefaultAssistantDriver().getFullName() : null)
                            .alreadyExists(exists)
                            .statusText(exists ? "ĐÃ TỒN TẠI (Bỏ qua)" : "HỢP LỆ (Sẽ tạo mới)")
                            .build());
                }
                current = current.plusDays(1);
            }
        }

        return GenerateTripsPreviewResponse.builder()
                .totalDatesChecked(items.size())
                .willCreateCount(willCreateCount)
                .alreadyExistsCount(alreadyExistsCount)
                .items(items)
                .build();
    }

    @Transactional
    public GenerateTripsResultResponse executeGenerateTrips(GenerateTripsRequest request, CurrentUser currentUser) {
        validateDateRange(request.getFromDate(), request.getToDate());

        List<TripRun> runs = resolveTripRuns(request.getTripRunId());
        String username = currentUser != null ? currentUser.username() : "SYSTEM";

        int totalChecked = 0;
        int createdCount = 0;
        int skippedCount = 0;
        List<String> createdCodes = new ArrayList<>();

        for (TripRun run : runs) {
            LocalDate current = request.getFromDate();
            Set<Integer> activeDays = parseDaysOfWeek(run.getDaysOfWeek());

            while (!current.isAfter(request.getToDate())) {
                if (isDateEligible(run, current, activeDays)) {
                    totalChecked++;
                    OffsetDateTime depTime = current.atTime(run.getDepartureTime()).atOffset(VIETNAM_OFFSET);

                    // Chống trùng lặp (Idempotent check)
                    boolean exists = tripRepository.existsByTripRunIdAndDepartureTime(run.getId(), depTime);
                    if (exists) {
                        skippedCount++;
                        current = current.plusDays(1);
                        continue;
                    }

                    // Kiểm tra phương tiện và tài xế mặc định
                    if (run.getDefaultVehicle() == null || run.getDefaultDriver() == null || run.getDefaultAssistantDriver() == null) {
                        throw new BusinessRuleException("Lịch vòng chạy " + run.getCode()
                                + " chưa cấu hình đủ Xe, Tài xế chính và Phụ xe mặc định để tự động sinh chuyến");
                    }

                    int duration = (run.getRoute() != null && run.getRoute().getEstimatedDurationMinutes() != null)
                            ? run.getRoute().getEstimatedDurationMinutes() : 180;
                    OffsetDateTime arrTime = depTime.plusMinutes(duration);

                    String tripCode = generateTripCode(run.getCode(), current, run.getDepartureTime().toString());

                    Trip trip = Trip.builder()
                            .code(tripCode)
                            .tripRun(run)
                            .route(run.getRoute())
                            .vehicle(run.getDefaultVehicle())
                            .driver(run.getDefaultDriver())
                            .assistantDriver(run.getDefaultAssistantDriver())
                            .departureTime(depTime)
                            .estimatedArrivalTime(arrTime)
                            .basePrice(run.getBasePrice())
                            .status(TripStatus.SCHEDULED)
                            .note("Sinh tự động từ lịch vòng chạy: " + run.getName())
                            .createdBy(username)
                            .updatedBy(username)
                            .build();

                    Trip saved = tripRepository.save(trip);
                    createdCodes.add(saved.getCode());
                    createdCount++;
                }
                current = current.plusDays(1);
            }
        }

        String message = String.format(
                "Hoàn thành tác vụ sinh chuyến: Đã tạo mới %d chuyến, bỏ qua %d chuyến đã tồn tại trước đó.",
                createdCount, skippedCount
        );
        log.info("Kết quả sinh chuyến: {}", message);

        return GenerateTripsResultResponse.builder()
                .totalDatesChecked(totalChecked)
                .createdCount(createdCount)
                .skippedCount(skippedCount)
                .message(message)
                .createdTripCodes(createdCodes)
                .build();
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new BusinessRuleException("Ngày kết thúc không được trước ngày bắt đầu");
        }
        long daysBetween = ChronoUnit.DAYS.between(from, to) + 1;
        if (daysBetween > MAX_GENERATE_DAYS) {
            throw new BusinessRuleException("Khoảng thời gian sinh chuyến tối đa là " + MAX_GENERATE_DAYS
                    + " ngày (hiện tại: " + daysBetween + " ngày)");
        }
    }

    private List<TripRun> resolveTripRuns(UUID tripRunId) {
        if (tripRunId != null) {
            TripRun run = tripRunRepository.findById(tripRunId)
                    .orElseThrow(() -> new ResourceNotFoundException("lịch vòng chạy", tripRunId));
            if (run.getStatus() != TripRunStatus.ACTIVE) {
                throw new BusinessRuleException("Lịch vòng chạy " + run.getCode() + " đang không ở trạng thái ACTIVE");
            }
            return Collections.singletonList(run);
        }
        return tripRunRepository.findByStatus(TripRunStatus.ACTIVE);
    }

    private boolean isDateEligible(TripRun run, LocalDate date, Set<Integer> activeDays) {
        if (date.isBefore(run.getStartDate())) return false;
        if (run.getEndDate() != null && date.isAfter(run.getEndDate())) return false;
        return activeDays.contains(date.getDayOfWeek().getValue());
    }

    private Set<Integer> parseDaysOfWeek(String daysCsv) {
        if (daysCsv == null || daysCsv.isBlank()) {
            return Set.of(1, 2, 3, 4, 5, 6, 7);
        }
        return Arrays.stream(daysCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }

    private String generateTripCode(String runCode, LocalDate date, String timeStr) {
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        String cleanTime = timeStr.replace(":", "").substring(0, 4);
        return runCode + "-" + date.format(dateFmt) + "-" + cleanTime;
    }
}
