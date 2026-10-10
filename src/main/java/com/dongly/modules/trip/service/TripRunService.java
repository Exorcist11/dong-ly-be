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
import com.dongly.modules.trip.dto.CreateTripRunRequest;
import com.dongly.modules.trip.dto.TripRunResponse;
import com.dongly.modules.trip.dto.UpdateTripRunRequest;
import com.dongly.modules.trip.entity.TripRun;
import com.dongly.modules.trip.entity.TripRunStatus;
import com.dongly.modules.trip.repository.TripRepository;
import com.dongly.modules.trip.repository.TripRunRepository;
import com.dongly.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class TripRunService {

    private final TripRunRepository tripRunRepository;
    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public TripRunService(
            TripRunRepository tripRunRepository,
            TripRepository tripRepository,
            RouteRepository routeRepository,
            VehicleRepository vehicleRepository,
            DriverRepository driverRepository
    ) {
        this.tripRunRepository = tripRunRepository;
        this.tripRepository = tripRepository;
        this.routeRepository = routeRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
    }

    @Transactional(readOnly = true)
    public Page<TripRunResponse> searchTripRuns(
            String keyword,
            UUID routeId,
            TripRunStatus status,
            Pageable pageable
    ) {
        String cleanKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return tripRunRepository.searchTripRuns(cleanKeyword, routeId, status, pageable)
                .map(TripRunResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public TripRunResponse getTripRunById(UUID id) {
        TripRun run = tripRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("lịch vòng chạy", id));
        return TripRunResponse.fromEntity(run);
    }

    @Transactional
    public TripRunResponse createTripRun(CreateTripRunRequest request, CurrentUser currentUser) {
        if (tripRunRepository.existsByCode(request.getCode().trim())) {
            throw new BusinessRuleException("Mã lịch vòng chạy '" + request.getCode() + "' đã tồn tại trong hệ thống");
        }

        validateDateRange(request.getStartDate(), request.getEndDate());

        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("tuyến đường", request.getRouteId()));
        if (route.getStatus() != CommonStatus.ACTIVE) {
            throw new BusinessRuleException("Tuyến đường đang không hoạt động (INACTIVE), không thể lập lịch");
        }

        Vehicle defaultVehicle = resolveVehicle(request.getDefaultVehicleId());
        Driver defaultDriver = resolveDriver(request.getDefaultDriverId(), "tài xế chính");
        Driver defaultAssistantDriver = resolveDriver(request.getDefaultAssistantDriverId(), "phụ xe");

        validateDefaultDrivers(defaultDriver, defaultAssistantDriver);

        String username = currentUser != null ? currentUser.username() : "SYSTEM";

        TripRun tripRun = TripRun.builder()
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .route(route)
                .departureTime(request.getDepartureTime())
                .daysOfWeek(request.getDaysOfWeek().trim())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .defaultVehicle(defaultVehicle)
                .defaultDriver(defaultDriver)
                .defaultAssistantDriver(defaultAssistantDriver)
                .basePrice(request.getBasePrice())
                .status(request.getStatus() != null ? request.getStatus() : TripRunStatus.ACTIVE)
                .note(request.getNote())
                .createdBy(username)
                .updatedBy(username)
                .build();

        TripRun saved = tripRunRepository.save(tripRun);
        log.info("Đã tạo mới lịch vòng chạy: id={}, code={}", saved.getId(), saved.getCode());
        return TripRunResponse.fromEntity(saved);
    }

    @Transactional
    public TripRunResponse updateTripRun(UUID id, UpdateTripRunRequest request, CurrentUser currentUser) {
        TripRun tripRun = tripRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("lịch vòng chạy", id));

        validateDateRange(request.getStartDate(), request.getEndDate());

        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("tuyến đường", request.getRouteId()));
        if (route.getStatus() != CommonStatus.ACTIVE) {
            throw new BusinessRuleException("Tuyến đường đang không hoạt động (INACTIVE), không thể gắn vào lịch chạy");
        }

        Vehicle defaultVehicle = resolveVehicle(request.getDefaultVehicleId());
        Driver defaultDriver = resolveDriver(request.getDefaultDriverId(), "tài xế chính");
        Driver defaultAssistantDriver = resolveDriver(request.getDefaultAssistantDriverId(), "phụ xe");

        validateDefaultDrivers(defaultDriver, defaultAssistantDriver);

        String username = currentUser != null ? currentUser.username() : "SYSTEM";

        tripRun.setName(request.getName().trim());
        tripRun.setRoute(route);
        tripRun.setDepartureTime(request.getDepartureTime());
        tripRun.setDaysOfWeek(request.getDaysOfWeek().trim());
        tripRun.setStartDate(request.getStartDate());
        tripRun.setEndDate(request.getEndDate());
        tripRun.setDefaultVehicle(defaultVehicle);
        tripRun.setDefaultDriver(defaultDriver);
        tripRun.setDefaultAssistantDriver(defaultAssistantDriver);
        tripRun.setBasePrice(request.getBasePrice());
        if (request.getStatus() != null) {
            tripRun.setStatus(request.getStatus());
        }
        tripRun.setNote(request.getNote());
        tripRun.setUpdatedBy(username);

        TripRun updated = tripRunRepository.save(tripRun);
        log.info("Đã cập nhật lịch vòng chạy: id={}, code={}", updated.getId(), updated.getCode());
        return TripRunResponse.fromEntity(updated);
    }

    @Transactional
    public TripRunResponse updateStatus(UUID id, TripRunStatus status, CurrentUser currentUser) {
        TripRun tripRun = tripRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("lịch vòng chạy", id));

        tripRun.setStatus(status);
        if (currentUser != null) {
            tripRun.setUpdatedBy(currentUser.username());
        }

        TripRun updated = tripRunRepository.save(tripRun);
        log.info("Cập nhật trạng thái lịch vòng chạy: id={}, status={}", id, status);
        return TripRunResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteTripRun(UUID id) {
        TripRun tripRun = tripRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("lịch vòng chạy", id));

        long linkedTripsCount = tripRepository.countByTripRunId(id);
        if (linkedTripsCount > 0) {
            throw new BusinessRuleException("Không thể xóa lịch vòng chạy đã có " + linkedTripsCount
                    + " chuyến xe phát sinh. Hãy chuyển trạng thái sang INACTIVE để dừng sinh chuyến mới.");
        }

        tripRunRepository.delete(tripRun);
        log.info("Đã xóa lịch vòng chạy: id={}, code={}", id, tripRun.getCode());
    }

    private void validateDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessRuleException("Ngày kết thúc áp dụng không được trước ngày bắt đầu áp dụng");
        }
    }

    private void validateDefaultDrivers(Driver driver, Driver assistant) {
        if (driver != null && assistant != null && Objects.equals(driver.getId(), assistant.getId())) {
            throw new BusinessRuleException("Tài xế chính và phụ xe mặc định không được là cùng một người");
        }
    }

    private Vehicle resolveVehicle(UUID vehicleId) {
        if (vehicleId == null) return null;
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("phương tiện", vehicleId));
        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new BusinessRuleException("Phương tiện " + vehicle.getPlateNumber() + " đang không ở trạng thái ACTIVE");
        }
        return vehicle;
    }

    private Driver resolveDriver(UUID driverId, String roleTitle) {
        if (driverId == null) return null;
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException(roleTitle, driverId));
        if (driver.getStatus() != DriverStatus.ACTIVE) {
            throw new BusinessRuleException("Tài xế " + driver.getFullName() + " (" + roleTitle + ") đang không ở trạng thái sẵn sàng (ACTIVE)");
        }
        return driver;
    }
}
