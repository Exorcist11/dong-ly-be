package com.dongly.modules.route.service;

import com.dongly.common.exception.AppException;
import com.dongly.common.exception.BusinessRuleException;
import com.dongly.common.exception.ErrorCode;
import com.dongly.common.exception.ResourceNotFoundException;
import com.dongly.modules.route.dto.CreateRouteRequest;
import com.dongly.modules.route.dto.LocationResponse;
import com.dongly.modules.route.dto.RouteDetailResponse;
import com.dongly.modules.route.dto.RouteStopInputDto;
import com.dongly.modules.route.dto.RouteStopResponse;
import com.dongly.modules.route.dto.RouteSummaryResponse;
import com.dongly.modules.route.dto.UpdateRouteRequest;
import com.dongly.modules.route.dto.UpdateRouteStopsRequest;
import com.dongly.modules.route.dto.UpdateStatusRequest;
import com.dongly.modules.route.entity.CommonStatus;
import com.dongly.modules.route.entity.Location;
import com.dongly.modules.route.entity.Route;
import com.dongly.modules.route.entity.RouteDirectionType;
import com.dongly.modules.route.entity.RouteStop;
import com.dongly.modules.route.entity.RouteStopType;
import com.dongly.modules.route.entity.StopPoint;
import com.dongly.modules.route.repository.LocationRepository;
import com.dongly.modules.route.repository.RouteRepository;
import com.dongly.modules.route.repository.RouteStopRepository;
import com.dongly.modules.route.repository.StopPointRepository;
import com.dongly.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RouteService {

    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final LocationRepository locationRepository;
    private final StopPointRepository stopPointRepository;

    public RouteService(
            RouteRepository routeRepository,
            RouteStopRepository routeStopRepository,
            LocationRepository locationRepository,
            StopPointRepository stopPointRepository
    ) {
        this.routeRepository = routeRepository;
        this.routeStopRepository = routeStopRepository;
        this.locationRepository = locationRepository;
        this.stopPointRepository = stopPointRepository;
    }

    @Transactional(readOnly = true)
    public Page<RouteSummaryResponse> searchRoutes(
            String keyword,
            UUID originLocationId,
            UUID destinationLocationId,
            CommonStatus status,
            Pageable pageable
    ) {
        return routeRepository.searchRoutes(keyword, originLocationId, destinationLocationId, status, pageable)
                .map(this::mapToRouteSummaryResponse);
    }

    @Transactional(readOnly = true)
    public RouteDetailResponse getRouteById(UUID id) {
        Route route = routeRepository.findByIdWithStops(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tuyến đường với ID: " + id));
        return mapToRouteDetailResponse(route);
    }

    @Transactional
    public RouteDetailResponse createRoute(CreateRouteRequest request, CurrentUser currentUser) {
        String normalizedCode = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (routeRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new AppException(ErrorCode.RESOURCE_ALREADY_EXISTS,
                    "Mã tuyến đường '" + normalizedCode + "' đã tồn tại trong hệ thống");
        }

        if (Objects.equals(request.getOriginLocationId(), request.getDestinationLocationId())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Điểm khởi hành và điểm đến không được trùng nhau");
        }

        Location origin = locationRepository.findById(request.getOriginLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa phương khởi hành: " + request.getOriginLocationId()));
        Location destination = locationRepository.findById(request.getDestinationLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa phương đến: " + request.getDestinationLocationId()));

        String username = currentUser != null ? currentUser.username() : "system";

        Route route = Route.builder()
                .code(normalizedCode)
                .name(request.getName().trim())
                .originLocation(origin)
                .destinationLocation(destination)
                .distanceKm(request.getDistanceKm())
                .estimatedDurationMinutes(request.getEstimatedDurationMinutes())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .status(CommonStatus.ACTIVE)
                .createdBy(username)
                .updatedBy(username)
                .build();

        Route savedRoute = routeRepository.save(route);

        if (request.getStops() != null && !request.getStops().isEmpty()) {
            validateAndAttachStops(savedRoute, request.getStops(), username);
            savedRoute = routeRepository.save(savedRoute);
        }

        log.info("Tạo mới tuyến đường thành công: id={}, code={}, createdBy={}",
                savedRoute.getId(), savedRoute.getCode(), username);
        return getRouteById(savedRoute.getId());
    }

    @Transactional
    public RouteDetailResponse updateRoute(UUID id, UpdateRouteRequest request, CurrentUser currentUser) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tuyến đường với ID: " + id));

        String normalizedCode = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (routeRepository.existsByCodeIgnoreCaseAndIdNot(normalizedCode, id)) {
            throw new AppException(ErrorCode.RESOURCE_ALREADY_EXISTS,
                    "Mã tuyến đường '" + normalizedCode + "' đã tồn tại trong hệ thống");
        }

        if (Objects.equals(request.getOriginLocationId(), request.getDestinationLocationId())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Điểm khởi hành và điểm đến không được trùng nhau");
        }

        Location origin = locationRepository.findById(request.getOriginLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa phương khởi hành: " + request.getOriginLocationId()));
        Location destination = locationRepository.findById(request.getDestinationLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa phương đến: " + request.getDestinationLocationId()));

        String username = currentUser != null ? currentUser.username() : "system";

        route.setCode(normalizedCode);
        route.setName(request.getName().trim());
        route.setOriginLocation(origin);
        route.setDestinationLocation(destination);
        route.setDistanceKm(request.getDistanceKm());
        route.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        route.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        route.setUpdatedBy(username);

        routeRepository.save(route);
        log.info("Cập nhật tuyến đường thành công: id={}, code={}, updatedBy={}", route.getId(), route.getCode(), username);
        return getRouteById(route.getId());
    }

    @Transactional
    public RouteDetailResponse updateRouteStatus(UUID id, UpdateStatusRequest request, CurrentUser currentUser) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tuyến đường với ID: " + id));

        String username = currentUser != null ? currentUser.username() : "system";
        route.setStatus(request.getStatus());
        route.setUpdatedBy(username);

        routeRepository.save(route);
        log.info("Cập nhật trạng thái tuyến đường thành công: id={}, status={}, updatedBy={}",
                route.getId(), route.getStatus(), username);
        return getRouteById(route.getId());
    }

    @Transactional
    public RouteDetailResponse updateRouteStops(UUID id, UpdateRouteStopsRequest request, CurrentUser currentUser) {
        Route route = routeRepository.findByIdWithStops(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tuyến đường với ID: " + id));

        String username = currentUser != null ? currentUser.username() : "system";

        // Xóa sạch các stop cũ và flush để tránh xung đột unique sequence
        route.getStops().clear();
        routeRepository.saveAndFlush(route);

        if (request.getStops() != null && !request.getStops().isEmpty()) {
            validateAndAttachStops(route, request.getStops(), username);
            routeRepository.save(route);
        }

        log.info("Cập nhật danh sách điểm dừng cho tuyến thành công: routeId={}, count={}, updatedBy={}",
                route.getId(), (request.getStops() != null ? request.getStops().size() : 0), username);
        return getRouteById(route.getId());
    }

    private void validateAndAttachStops(Route route, List<RouteStopInputDto> inputStops, String username) {
        if (inputStops == null || inputStops.isEmpty()) {
            return;
        }

        // Nhóm trạm theo chiều
        Map<RouteDirectionType, List<RouteStopInputDto>> byDirection = inputStops.stream()
                .collect(Collectors.groupingBy(RouteStopInputDto::getDirection));

        for (Map.Entry<RouteDirectionType, List<RouteStopInputDto>> entry : byDirection.entrySet()) {
            RouteDirectionType direction = entry.getKey();
            List<RouteStopInputDto> stops = entry.getValue();

            // Kiểm tra trùng stopPoint trong cùng 1 chiều
            Set<UUID> stopPointIds = new HashSet<>();
            for (RouteStopInputDto stop : stops) {
                if (!stopPointIds.add(stop.getStopPointId())) {
                    throw new BusinessRuleException(
                            "Điểm dừng bị trùng lặp trên cùng chiều " + direction + ": ID " + stop.getStopPointId());
                }
            }

            // Sắp xếp theo sequence
            stops.sort(Comparator.comparing(RouteStopInputDto::getSequence));

            // Kiểm tra sequence liên tục bắt đầu từ 1
            for (int i = 0; i < stops.size(); i++) {
                int expectedSeq = i + 1;
                if (stops.get(i).getSequence() != expectedSeq) {
                    throw new BusinessRuleException(
                            "Thứ tự điểm dừng trên chiều " + direction + " phải liên tục bắt đầu từ 1 (kỳ vọng " + expectedSeq + " nhưng nhận " + stops.get(i).getSequence() + ")");
                }
            }

            // Kiểm tra vai trò điểm dừng đầu và cuối
            RouteStopInputDto firstStop = stops.get(0);
            if (firstStop.getStopType() == RouteStopType.DROPOFF) {
                throw new BusinessRuleException(
                        "Điểm dừng đầu tiên (thứ tự 1) trên chiều " + direction + " không thể chỉ có vai trò trả khách (DROPOFF)");
            }

            RouteStopInputDto lastStop = stops.get(stops.size() - 1);
            if (lastStop.getStopType() == RouteStopType.PICKUP) {
                throw new BusinessRuleException(
                        "Điểm dừng cuối cùng trên chiều " + direction + " không thể chỉ có vai trò đón khách (PICKUP)");
            }
        }

        // Tải các StopPoint tương ứng
        Set<UUID> allPointIds = inputStops.stream().map(RouteStopInputDto::getStopPointId).collect(Collectors.toSet());
        List<StopPoint> stopPoints = stopPointRepository.findAllById(allPointIds);
        Map<UUID, StopPoint> pointMap = stopPoints.stream().collect(Collectors.toMap(StopPoint::getId, p -> p));

        if (pointMap.size() != allPointIds.size()) {
            throw new ResourceNotFoundException("Một hoặc nhiều điểm đón/trả vật lý không tồn tại");
        }

        for (RouteStopInputDto dto : inputStops) {
            StopPoint sp = pointMap.get(dto.getStopPointId());
            RouteStop stop = RouteStop.builder()
                    .route(route)
                    .stopPoint(sp)
                    .direction(dto.getDirection())
                    .sequence(dto.getSequence())
                    .stopType(dto.getStopType())
                    .extraPrice(dto.getExtraPrice() != null ? dto.getExtraPrice() : BigDecimal.ZERO)
                    .status(CommonStatus.ACTIVE)
                    .createdBy(username)
                    .updatedBy(username)
                    .build();
            route.getStops().add(stop);
        }
    }

    private RouteSummaryResponse mapToRouteSummaryResponse(Route route) {
        return RouteSummaryResponse.builder()
                .id(route.getId())
                .code(route.getCode())
                .name(route.getName())
                .originLocation(mapToLocationResponse(route.getOriginLocation()))
                .destinationLocation(mapToLocationResponse(route.getDestinationLocation()))
                .distanceKm(route.getDistanceKm())
                .estimatedDurationMinutes(route.getEstimatedDurationMinutes())
                .totalStops(route.getStops() != null ? route.getStops().size() : 0)
                .status(route.getStatus())
                .createdBy(route.getCreatedBy())
                .updatedBy(route.getUpdatedBy())
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .build();
    }

    private RouteDetailResponse mapToRouteDetailResponse(Route route) {
        List<RouteStopResponse> stopResponses = new ArrayList<>();
        if (route.getStops() != null) {
            stopResponses = route.getStops().stream()
                    .sorted(Comparator.comparing(RouteStop::getDirection).thenComparing(RouteStop::getSequence))
                    .map(this::mapToRouteStopResponse)
                    .toList();
        }

        return RouteDetailResponse.builder()
                .id(route.getId())
                .code(route.getCode())
                .name(route.getName())
                .originLocation(mapToLocationResponse(route.getOriginLocation()))
                .destinationLocation(mapToLocationResponse(route.getDestinationLocation()))
                .distanceKm(route.getDistanceKm())
                .estimatedDurationMinutes(route.getEstimatedDurationMinutes())
                .description(route.getDescription())
                .status(route.getStatus())
                .stops(stopResponses)
                .createdBy(route.getCreatedBy())
                .updatedBy(route.getUpdatedBy())
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .build();
    }

    private RouteStopResponse mapToRouteStopResponse(RouteStop stop) {
        StopPoint sp = stop.getStopPoint();
        return RouteStopResponse.builder()
                .id(stop.getId())
                .stopPointId(sp.getId())
                .stopPointCode(sp.getCode())
                .stopPointName(sp.getName())
                .address(sp.getAddress())
                .locationName(sp.getLocation() != null ? sp.getLocation().getName() : null)
                .direction(stop.getDirection())
                .sequence(stop.getSequence())
                .stopType(stop.getStopType())
                .extraPrice(stop.getExtraPrice())
                .status(stop.getStatus())
                .build();
    }

    private LocationResponse mapToLocationResponse(Location location) {
        if (location == null) return null;
        return LocationResponse.builder()
                .id(location.getId())
                .code(location.getCode())
                .name(location.getName())
                .province(location.getProvince())
                .status(location.getStatus())
                .build();
    }
}
