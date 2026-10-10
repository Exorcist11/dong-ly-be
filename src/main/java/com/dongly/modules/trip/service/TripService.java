package com.dongly.modules.trip.service;

import com.dongly.common.exception.BusinessRuleException;
import com.dongly.common.exception.ResourceNotFoundException;
import com.dongly.modules.fleet.entity.Driver;
import com.dongly.modules.fleet.entity.DriverStatus;
import com.dongly.modules.fleet.entity.Vehicle;
import com.dongly.modules.fleet.entity.VehicleStatus;
import com.dongly.modules.fleet.repository.DriverRepository;
import com.dongly.modules.fleet.repository.VehicleRepository;
import com.dongly.modules.route.entity.CommonStatus;
import com.dongly.modules.route.entity.Route;
import com.dongly.modules.route.repository.RouteRepository;
import com.dongly.modules.trip.dto.ConflictCheckRequest;
import com.dongly.modules.trip.dto.ConflictCheckResponse;
import com.dongly.modules.trip.dto.CreateTripRequest;
import com.dongly.modules.trip.dto.TripResponse;
import com.dongly.modules.trip.dto.UpdateTripRequest;
import com.dongly.modules.trip.dto.UpdateTripStatusRequest;
import com.dongly.modules.trip.entity.Trip;
import com.dongly.modules.trip.entity.TripRun;
import com.dongly.modules.trip.entity.TripStatus;
import com.dongly.modules.trip.repository.TripRepository;
import com.dongly.modules.trip.repository.TripRunRepository;
import com.dongly.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class TripService {

    public static final int BUFFER_MINUTES = 45;

    private final TripRepository tripRepository;
    private final TripRunRepository tripRunRepository;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public TripService(
            TripRepository tripRepository,
            TripRunRepository tripRunRepository,
            RouteRepository routeRepository,
            VehicleRepository vehicleRepository,
            DriverRepository driverRepository
    ) {
        this.tripRepository = tripRepository;
        this.tripRunRepository = tripRunRepository;
        this.routeRepository = routeRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> searchTrips(
            String keyword,
            UUID routeId,
            UUID vehicleId,
            UUID driverId,
            TripStatus status,
            OffsetDateTime fromDate,
            OffsetDateTime toDate,
            Pageable pageable
    ) {
        String cleanKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return tripRepository.searchTrips(
                cleanKeyword, routeId, vehicleId, driverId, status, fromDate, toDate, pageable
        ).map(TripResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public TripResponse getTripById(UUID id) {
        Trip trip = tripRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("chuyến xe", id));
        return TripResponse.fromEntity(trip);
    }

    @Transactional(readOnly = true)
    public ConflictCheckResponse checkConflict(ConflictCheckRequest request) {
        return evaluateConflicts(
                request.getTripId(),
                request.getVehicleId(),
                request.getDriverId(),
                request.getAssistantDriverId(),
                request.getDepartureTime(),
                request.getEstimatedArrivalTime()
        );
    }

    @Transactional
    public TripResponse createTrip(CreateTripRequest request, CurrentUser currentUser) {
        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("tuyến đường", request.getRouteId()));
        if (route.getStatus() != CommonStatus.ACTIVE) {
            throw new BusinessRuleException("Tuyến đường đang không hoạt động (INACTIVE), không thể tạo chuyến");
        }

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("phương tiện", request.getVehicleId()));
        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new BusinessRuleException("Phương tiện " + vehicle.getPlateNumber() + " đang không ở trạng thái ACTIVE");
        }

        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("tài xế chính", request.getDriverId()));
        if (driver.getStatus() != DriverStatus.ACTIVE) {
            throw new BusinessRuleException("Tài xế chính " + driver.getFullName() + " đang không ở trạng thái ACTIVE");
        }

        Driver assistant = driverRepository.findById(request.getAssistantDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("phụ xe", request.getAssistantDriverId()));
        if (assistant.getStatus() != DriverStatus.ACTIVE) {
            throw new BusinessRuleException("Phụ xe " + assistant.getFullName() + " đang không ở trạng thái ACTIVE");
        }

        if (Objects.equals(driver.getId(), assistant.getId())) {
            throw new BusinessRuleException("Tài xế chính và phụ xe không được là cùng một người");
        }

        OffsetDateTime departureTime = request.getDepartureTime();
        OffsetDateTime arrivalTime = request.getEstimatedArrivalTime();
        if (arrivalTime == null) {
            int duration = (route.getEstimatedDurationMinutes() != null && route.getEstimatedDurationMinutes() > 0)
                    ? route.getEstimatedDurationMinutes() : 180;
            arrivalTime = departureTime.plusMinutes(duration);
        }

        if (!arrivalTime.isAfter(departureTime)) {
            throw new BusinessRuleException("Thời điểm đến dự kiến phải sau thời điểm xuất bến");
        }

        // Kiểm tra xung đột điều phối (xe, tài xế chính, phụ xe)
        ConflictCheckResponse conflict = evaluateConflicts(
                null, vehicle.getId(), driver.getId(), assistant.getId(), departureTime, arrivalTime
        );
        if (conflict.isHasConflict()) {
            throw new BusinessRuleException(String.join(". ", conflict.getConflictMessages()));
        }

        TripRun tripRun = null;
        if (request.getTripRunId() != null) {
            tripRun = tripRunRepository.findById(request.getTripRunId()).orElse(null);
        }

        String tripCode = request.getCode();
        if (tripCode == null || tripCode.isBlank()) {
            tripCode = generateTripCode(departureTime, vehicle.getPlateNumber());
        } else {
            tripCode = tripCode.trim().toUpperCase();
            if (tripRepository.existsByCode(tripCode)) {
                throw new BusinessRuleException("Mã chuyến xe '" + tripCode + "' đã tồn tại");
            }
        }

        String username = currentUser != null ? currentUser.username() : "SYSTEM";
        TripStatus initialStatus = request.getStatus() != null ? request.getStatus() : TripStatus.SCHEDULED;

        Trip trip = Trip.builder()
                .code(tripCode)
                .tripRun(tripRun)
                .route(route)
                .vehicle(vehicle)
                .driver(driver)
                .assistantDriver(assistant)
                .departureTime(departureTime)
                .estimatedArrivalTime(arrivalTime)
                .basePrice(request.getBasePrice())
                .status(initialStatus)
                .note(request.getNote())
                .createdBy(username)
                .updatedBy(username)
                .build();

        Trip saved = tripRepository.save(trip);
        log.info("Đã tạo chuyến xe mới: id={}, code={}", saved.getId(), saved.getCode());
        return TripResponse.fromEntity(saved);
    }

    @Transactional
    public TripResponse updateTrip(UUID id, UpdateTripRequest request, CurrentUser currentUser) {
        Trip trip = tripRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("chuyến xe", id));

        if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
            throw new BusinessRuleException("Không thể chỉnh sửa chuyến xe đã kết thúc hoặc đã hủy");
        }

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("phương tiện", request.getVehicleId()));
        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new BusinessRuleException("Phương tiện " + vehicle.getPlateNumber() + " đang không ở trạng thái ACTIVE");
        }

        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("tài xế chính", request.getDriverId()));
        if (driver.getStatus() != DriverStatus.ACTIVE) {
            throw new BusinessRuleException("Tài xế chính " + driver.getFullName() + " đang không ở trạng thái ACTIVE");
        }

        Driver assistant = driverRepository.findById(request.getAssistantDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("phụ xe", request.getAssistantDriverId()));
        if (assistant.getStatus() != DriverStatus.ACTIVE) {
            throw new BusinessRuleException("Phụ xe " + assistant.getFullName() + " đang không ở trạng thái ACTIVE");
        }

        if (Objects.equals(driver.getId(), assistant.getId())) {
            throw new BusinessRuleException("Tài xế chính và phụ xe không được là cùng một người");
        }

        OffsetDateTime departureTime = request.getDepartureTime();
        OffsetDateTime arrivalTime = request.getEstimatedArrivalTime();
        if (arrivalTime == null) {
            int duration = (trip.getRoute().getEstimatedDurationMinutes() != null
                    && trip.getRoute().getEstimatedDurationMinutes() > 0)
                    ? trip.getRoute().getEstimatedDurationMinutes() : 180;
            arrivalTime = departureTime.plusMinutes(duration);
        }

        if (!arrivalTime.isAfter(departureTime)) {
            throw new BusinessRuleException("Thời điểm đến dự kiến phải sau thời điểm xuất bến");
        }

        ConflictCheckResponse conflict = evaluateConflicts(
                trip.getId(), vehicle.getId(), driver.getId(), assistant.getId(), departureTime, arrivalTime
        );
        if (conflict.isHasConflict()) {
            throw new BusinessRuleException(String.join(". ", conflict.getConflictMessages()));
        }

        String username = currentUser != null ? currentUser.username() : "SYSTEM";

        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip.setAssistantDriver(assistant);
        trip.setDepartureTime(departureTime);
        trip.setEstimatedArrivalTime(arrivalTime);
        trip.setBasePrice(request.getBasePrice());
        trip.setNote(request.getNote());
        trip.setUpdatedBy(username);

        Trip updated = tripRepository.save(trip);
        log.info("Đã cập nhật chuyến xe: id={}, code={}", updated.getId(), updated.getCode());
        return TripResponse.fromEntity(updated);
    }

    @Transactional
    public TripResponse updateStatus(UUID id, UpdateTripStatusRequest request, CurrentUser currentUser) {
        Trip trip = tripRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("chuyến xe", id));

        TripStatus currentStatus = trip.getStatus();
        TripStatus nextStatus = request.getStatus();

        validateStatusTransition(currentStatus, nextStatus);

        OffsetDateTime now = OffsetDateTime.now();
        if (nextStatus == TripStatus.DEPARTED) {
            trip.setActualDepartureTime(request.getActualDepartureTime() != null
                    ? request.getActualDepartureTime() : now);
        } else if (nextStatus == TripStatus.COMPLETED) {
            trip.setActualArrivalTime(request.getActualArrivalTime() != null
                    ? request.getActualArrivalTime() : now);
            if (trip.getActualDepartureTime() == null) {
                trip.setActualDepartureTime(trip.getDepartureTime());
            }
        }

        trip.setStatus(nextStatus);
        if (request.getNote() != null && !request.getNote().isBlank()) {
            trip.setNote(request.getNote());
        }
        if (currentUser != null) {
            trip.setUpdatedBy(currentUser.username());
        }

        Trip updated = tripRepository.save(trip);
        log.info("Chuyển trạng thái chuyến xe: id={}, code={}, {} -> {}",
                updated.getId(), updated.getCode(), currentStatus, nextStatus);
        return TripResponse.fromEntity(updated);
    }

    private void validateStatusTransition(TripStatus current, TripStatus next) {
        if (current == next) return;

        boolean valid = switch (current) {
            case SCHEDULED -> next == TripStatus.READY || next == TripStatus.CANCELLED;
            case READY -> next == TripStatus.DEPARTED || next == TripStatus.CANCELLED || next == TripStatus.SCHEDULED;
            case DEPARTED -> next == TripStatus.COMPLETED || next == TripStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };

        if (!valid) {
            throw new BusinessRuleException("Không thể chuyển trạng thái chuyến xe từ " + current + " sang " + next);
        }
    }

    private ConflictCheckResponse evaluateConflicts(
            UUID excludeTripId,
            UUID vehicleId,
            UUID driverId,
            UUID assistantDriverId,
            OffsetDateTime depTime,
            OffsetDateTime arrTime
    ) {
        ConflictCheckResponse response = ConflictCheckResponse.ok();
        List<String> messages = new ArrayList<>();
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm dd/MM");

        OffsetDateTime windowStart = depTime.minusMinutes(BUFFER_MINUTES);
        OffsetDateTime windowEnd = arrTime.plusMinutes(BUFFER_MINUTES);

        // 1. Kiểm tra Phương tiện
        if (vehicleId != null) {
            List<Trip> vehicleTrips = tripRepository.findActiveTripsByVehicleInWindow(
                    vehicleId, windowStart, windowEnd, excludeTripId
            );
            for (Trip t : vehicleTrips) {
                if (hasTimeOverlap(depTime, arrTime, t.getDepartureTime(), t.getEstimatedArrivalTime(), BUFFER_MINUTES)) {
                    response.setHasConflict(true);
                    response.setVehicleConflict(true);
                    messages.add("Phương tiện " + t.getVehicle().getPlateNumber() + " đang bận chuyến "
                            + t.getCode() + " (" + t.getDepartureTime().format(timeFmt) + " - "
                            + t.getEstimatedArrivalTime().format(timeFmt) + "), cần cách nhau tối thiểu " + BUFFER_MINUTES + " phút");
                    break;
                }
            }
        }

        // 2. Kiểm tra Tài xế chính
        if (driverId != null) {
            List<Trip> driverTrips = tripRepository.findActiveTripsByDriverInWindow(
                    driverId, windowStart, windowEnd, excludeTripId
            );
            for (Trip t : driverTrips) {
                if (hasTimeOverlap(depTime, arrTime, t.getDepartureTime(), t.getEstimatedArrivalTime(), BUFFER_MINUTES)) {
                    response.setHasConflict(true);
                    response.setDriverConflict(true);
                    String name = (t.getDriver() != null && Objects.equals(t.getDriver().getId(), driverId))
                            ? t.getDriver().getFullName()
                            : (t.getAssistantDriver() != null ? t.getAssistantDriver().getFullName() : "Tài xế");
                    messages.add("Tài xế " + name + " đang tham gia chuyến "
                            + t.getCode() + " (" + t.getDepartureTime().format(timeFmt) + " - "
                            + t.getEstimatedArrivalTime().format(timeFmt) + "), cần nghỉ tối thiểu " + BUFFER_MINUTES + " phút");
                    break;
                }
            }
        }

        // 3. Kiểm tra Phụ xe
        if (assistantDriverId != null) {
            List<Trip> assistantTrips = tripRepository.findActiveTripsByDriverInWindow(
                    assistantDriverId, windowStart, windowEnd, excludeTripId
            );
            for (Trip t : assistantTrips) {
                if (hasTimeOverlap(depTime, arrTime, t.getDepartureTime(), t.getEstimatedArrivalTime(), BUFFER_MINUTES)) {
                    response.setHasConflict(true);
                    response.setAssistantDriverConflict(true);
                    String name = (t.getAssistantDriver() != null && Objects.equals(t.getAssistantDriver().getId(), assistantDriverId))
                            ? t.getAssistantDriver().getFullName()
                            : (t.getDriver() != null ? t.getDriver().getFullName() : "Phụ xe");
                    messages.add("Phụ xe " + name + " đang tham gia chuyến "
                            + t.getCode() + " (" + t.getDepartureTime().format(timeFmt) + " - "
                            + t.getEstimatedArrivalTime().format(timeFmt) + "), cần nghỉ tối thiểu " + BUFFER_MINUTES + " phút");
                    break;
                }
            }
        }

        response.setConflictMessages(messages);
        return response;
    }

    private boolean hasTimeOverlap(
            OffsetDateTime dep1, OffsetDateTime arr1,
            OffsetDateTime dep2, OffsetDateTime arr2,
            int bufferMinutes
    ) {
        OffsetDateTime end1WithBuffer = arr1.plusMinutes(bufferMinutes);
        OffsetDateTime end2WithBuffer = arr2.plusMinutes(bufferMinutes);
        return dep1.isBefore(end2WithBuffer) && end1WithBuffer.isAfter(dep2);
    }

    private String generateTripCode(OffsetDateTime departureTime, String plateNumber) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
        String timeStr = departureTime.format(fmt);
        String cleanPlate = plateNumber.replace("-", "").replace(".", "");
        String candidate = "TRP-" + timeStr + "-" + cleanPlate;
        if (!tripRepository.existsByCode(candidate)) {
            return candidate;
        }
        return candidate + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
