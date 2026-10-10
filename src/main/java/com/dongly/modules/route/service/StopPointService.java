package com.dongly.modules.route.service;

import com.dongly.common.exception.AppException;
import com.dongly.common.exception.BusinessRuleException;
import com.dongly.common.exception.ErrorCode;
import com.dongly.common.exception.ResourceNotFoundException;
import com.dongly.modules.route.dto.CreateStopPointRequest;
import com.dongly.modules.route.dto.LocationResponse;
import com.dongly.modules.route.dto.StopPointResponse;
import com.dongly.modules.route.dto.UpdateStatusRequest;
import com.dongly.modules.route.dto.UpdateStopPointRequest;
import com.dongly.modules.route.entity.CommonStatus;
import com.dongly.modules.route.entity.Location;
import com.dongly.modules.route.entity.StopPoint;
import com.dongly.modules.route.repository.LocationRepository;
import com.dongly.modules.route.repository.RouteStopRepository;
import com.dongly.modules.route.repository.StopPointRepository;
import com.dongly.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
public class StopPointService {

    private final StopPointRepository stopPointRepository;
    private final LocationRepository locationRepository;
    private final RouteStopRepository routeStopRepository;

    public StopPointService(
            StopPointRepository stopPointRepository,
            LocationRepository locationRepository,
            RouteStopRepository routeStopRepository
    ) {
        this.stopPointRepository = stopPointRepository;
        this.locationRepository = locationRepository;
        this.routeStopRepository = routeStopRepository;
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> getActiveLocations() {
        return locationRepository.findAllByStatusOrderByProvinceAscNameAsc(CommonStatus.ACTIVE)
                .stream()
                .map(this::mapToLocationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<StopPointResponse> searchStopPoints(
            String keyword,
            UUID locationId,
            CommonStatus status,
            Pageable pageable
    ) {
        return stopPointRepository.searchStopPoints(keyword, locationId, status, pageable)
                .map(this::mapToStopPointResponse);
    }

    @Transactional(readOnly = true)
    public StopPointResponse getStopPointById(UUID id) {
        StopPoint stopPoint = stopPointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy điểm đón/trả với ID: " + id));
        return mapToStopPointResponse(stopPoint);
    }

    @Transactional
    public StopPointResponse createStopPoint(CreateStopPointRequest request, CurrentUser currentUser) {
        String normalizedCode = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (stopPointRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new AppException(ErrorCode.RESOURCE_ALREADY_EXISTS,
                    "Mã điểm đón/trả '" + normalizedCode + "' đã tồn tại trong hệ thống");
        }

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa phương với ID: " + request.getLocationId()));

        if (location.getStatus() != CommonStatus.ACTIVE) {
            throw new BusinessRuleException("Không thể tạo điểm đón/trả thuộc địa phương đang ngừng hoạt động");
        }

        String username = currentUser != null ? currentUser.username() : "system";

        StopPoint stopPoint = StopPoint.builder()
                .code(normalizedCode)
                .name(request.getName().trim())
                .location(location)
                .address(request.getAddress().trim())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .contactPhone(request.getContactPhone() != null ? request.getContactPhone().trim() : null)
                .status(CommonStatus.ACTIVE)
                .createdBy(username)
                .updatedBy(username)
                .build();

        StopPoint saved = stopPointRepository.save(stopPoint);
        log.info("Tạo mới điểm đón/trả thành công: id={}, code={}, createdBy={}", saved.getId(), saved.getCode(), username);
        return mapToStopPointResponse(saved);
    }

    @Transactional
    public StopPointResponse updateStopPoint(UUID id, UpdateStopPointRequest request, CurrentUser currentUser) {
        StopPoint stopPoint = stopPointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy điểm đón/trả với ID: " + id));

        String normalizedCode = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (stopPointRepository.existsByCodeIgnoreCaseAndIdNot(normalizedCode, id)) {
            throw new AppException(ErrorCode.RESOURCE_ALREADY_EXISTS,
                    "Mã điểm đón/trả '" + normalizedCode + "' đã tồn tại trong hệ thống");
        }

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa phương với ID: " + request.getLocationId()));

        String username = currentUser != null ? currentUser.username() : "system";

        stopPoint.setCode(normalizedCode);
        stopPoint.setName(request.getName().trim());
        stopPoint.setLocation(location);
        stopPoint.setAddress(request.getAddress().trim());
        stopPoint.setLatitude(request.getLatitude());
        stopPoint.setLongitude(request.getLongitude());
        stopPoint.setContactPhone(request.getContactPhone() != null ? request.getContactPhone().trim() : null);
        stopPoint.setUpdatedBy(username);

        StopPoint saved = stopPointRepository.save(stopPoint);
        log.info("Cập nhật điểm đón/trả thành công: id={}, code={}, updatedBy={}", saved.getId(), saved.getCode(), username);
        return mapToStopPointResponse(saved);
    }

    @Transactional
    public StopPointResponse updateStatus(UUID id, UpdateStatusRequest request, CurrentUser currentUser) {
        StopPoint stopPoint = stopPointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy điểm đón/trả với ID: " + id));

        if (request.getStatus() == CommonStatus.INACTIVE) {
            long usedInRoutesCount = routeStopRepository.countByStopPointId(id);
            if (usedInRoutesCount > 0) {
                log.warn("Ngừng hoạt động điểm đón/trả id={} đang được sử dụng trong {} cấu hình trạm tuyến",
                        id, usedInRoutesCount);
            }
        }

        String username = currentUser != null ? currentUser.username() : "system";
        stopPoint.setStatus(request.getStatus());
        stopPoint.setUpdatedBy(username);

        StopPoint saved = stopPointRepository.save(stopPoint);
        log.info("Cập nhật trạng thái điểm đón/trả thành công: id={}, status={}, updatedBy={}",
                saved.getId(), saved.getStatus(), username);
        return mapToStopPointResponse(saved);
    }

    public StopPointResponse mapToStopPointResponse(StopPoint stopPoint) {
        return StopPointResponse.builder()
                .id(stopPoint.getId())
                .code(stopPoint.getCode())
                .name(stopPoint.getName())
                .locationId(stopPoint.getLocation() != null ? stopPoint.getLocation().getId() : null)
                .locationName(stopPoint.getLocation() != null ? stopPoint.getLocation().getName() : null)
                .locationProvince(stopPoint.getLocation() != null ? stopPoint.getLocation().getProvince() : null)
                .address(stopPoint.getAddress())
                .latitude(stopPoint.getLatitude())
                .longitude(stopPoint.getLongitude())
                .contactPhone(stopPoint.getContactPhone())
                .status(stopPoint.getStatus())
                .createdBy(stopPoint.getCreatedBy())
                .updatedBy(stopPoint.getUpdatedBy())
                .createdAt(stopPoint.getCreatedAt())
                .updatedAt(stopPoint.getUpdatedAt())
                .build();
    }

    public LocationResponse mapToLocationResponse(Location location) {
        return LocationResponse.builder()
                .id(location.getId())
                .code(location.getCode())
                .name(location.getName())
                .province(location.getProvince())
                .status(location.getStatus())
                .build();
    }
}
