package com.dongly.modules.fleet.service;

import com.dongly.common.exception.AppException;
import com.dongly.common.exception.BusinessRuleException;
import com.dongly.common.exception.ErrorCode;
import com.dongly.common.exception.ResourceNotFoundException;
import com.dongly.modules.fleet.dto.CreateVehicleRequest;
import com.dongly.modules.fleet.dto.UpdateVehicleRequest;
import com.dongly.modules.fleet.dto.VehicleDetailResponse;
import com.dongly.modules.fleet.dto.VehicleSeatResponse;
import com.dongly.modules.fleet.dto.VehicleSummaryResponse;
import com.dongly.modules.fleet.entity.Vehicle;
import com.dongly.modules.fleet.entity.VehicleSeat;
import com.dongly.modules.fleet.entity.VehicleStatus;
import com.dongly.modules.fleet.entity.VehicleType;
import com.dongly.modules.fleet.repository.VehicleRepository;
import com.dongly.modules.fleet.repository.VehicleSeatRepository;
import com.dongly.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleSeatRepository seatRepository;

    public VehicleService(VehicleRepository vehicleRepository, VehicleSeatRepository seatRepository) {
        this.vehicleRepository = vehicleRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional(readOnly = true)
    public Page<VehicleSummaryResponse> searchVehicles(
            String keyword,
            VehicleType vehicleType,
            VehicleStatus status,
            Pageable pageable
    ) {
        String cleanKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return vehicleRepository.searchVehicles(cleanKeyword, vehicleType, status, pageable)
                .map(VehicleSummaryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public VehicleDetailResponse getVehicleById(UUID id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("phương tiện", id));

        List<VehicleSeatResponse> seatResponses = seatRepository
                .findByVehicleIdOrderByFloorAscRowIndexAscColumnIndexAsc(vehicle.getId())
                .stream()
                .map(VehicleSeatResponse::fromEntity)
                .toList();

        return VehicleDetailResponse.of(vehicle, seatResponses);
    }

    @Transactional
    public VehicleDetailResponse createVehicle(CreateVehicleRequest request, CurrentUser currentUser) {
        String normalizedPlate = normalizePlateNumber(request.getPlateNumber());

        if (vehicleRepository.existsByPlateNumber(normalizedPlate)) {
            throw new AppException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Biển số xe đã tồn tại trong hệ thống: " + normalizedPlate);
        }

        String username = currentUser != null ? currentUser.username() : "SYSTEM";

        Vehicle vehicle = Vehicle.builder()
                .plateNumber(normalizedPlate)
                .vehicleType(request.getVehicleType())
                .brand(request.getBrand().trim())
                .model(request.getModel() != null ? request.getModel().trim() : null)
                .manufactureYear(request.getManufactureYear())
                .totalFloors(request.getTotalFloors())
                .totalRows(request.getTotalRows())
                .totalColumns(request.getTotalColumns())
                .totalSeats(0)
                .status(VehicleStatus.ACTIVE)
                .description(request.getDescription())
                .createdBy(username)
                .updatedBy(username)
                .build();

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        log.info("Đã tạo mới phương tiện [ID: {}, Biển số: {}] bởi {}", savedVehicle.getId(), savedVehicle.getPlateNumber(), username);

        return VehicleDetailResponse.of(savedVehicle, List.of());
    }

    @Transactional
    public VehicleDetailResponse updateVehicle(UUID id, UpdateVehicleRequest request, CurrentUser currentUser) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("phương tiện", id));

        String normalizedPlate = normalizePlateNumber(request.getPlateNumber());
        if (vehicleRepository.existsByPlateNumberAndIdNot(normalizedPlate, id)) {
            throw new AppException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Biển số xe đã được sử dụng bởi phương tiện khác: " + normalizedPlate);
        }

        // Kiểm tra nếu thu hẹp kích thước sơ đồ xe khiến các ghế hiện tại nằm ngoài phạm vi
        if (request.getTotalFloors() < vehicle.getTotalFloors() ||
            request.getTotalRows() < vehicle.getTotalRows() ||
            request.getTotalColumns() < vehicle.getTotalColumns()) {
            
            List<VehicleSeat> existingSeats = seatRepository.findByVehicleIdOrderByFloorAscRowIndexAscColumnIndexAsc(id);
            for (VehicleSeat seat : existingSeats) {
                if (seat.getFloor() > request.getTotalFloors() ||
                    seat.getRowIndex() > request.getTotalRows() ||
                    seat.getColumnIndex() > request.getTotalColumns()) {
                    throw new BusinessRuleException(
                            String.format("Không thể giảm kích thước sơ đồ vì ghế %s đang ở tọa độ (Tầng %d, Hàng %d, Cột %d) vượt quá giới hạn mới!",
                                    seat.getSeatCode(), seat.getFloor(), seat.getRowIndex(), seat.getColumnIndex()));
                }
            }
        }

        String username = currentUser != null ? currentUser.username() : "SYSTEM";

        vehicle.setPlateNumber(normalizedPlate);
        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setBrand(request.getBrand().trim());
        vehicle.setModel(request.getModel() != null ? request.getModel().trim() : null);
        vehicle.setManufactureYear(request.getManufactureYear());
        vehicle.setTotalFloors(request.getTotalFloors());
        vehicle.setTotalRows(request.getTotalRows());
        vehicle.setTotalColumns(request.getTotalColumns());
        vehicle.setDescription(request.getDescription());
        vehicle.setUpdatedBy(username);

        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        log.info("Đã cập nhật phương tiện [ID: {}] bởi {}", updatedVehicle.getId(), username);

        List<VehicleSeatResponse> seatResponses = seatRepository
                .findByVehicleIdOrderByFloorAscRowIndexAscColumnIndexAsc(updatedVehicle.getId())
                .stream()
                .map(VehicleSeatResponse::fromEntity)
                .toList();

        return VehicleDetailResponse.of(updatedVehicle, seatResponses);
    }

    @Transactional
    public VehicleDetailResponse updateStatus(UUID id, VehicleStatus newStatus, CurrentUser currentUser) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("phương tiện", id));

        String username = currentUser != null ? currentUser.username() : "SYSTEM";
        vehicle.setStatus(newStatus);
        vehicle.setUpdatedBy(username);

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Đã đổi trạng thái phương tiện [ID: {}] sang {} bởi {}", saved.getId(), newStatus, username);

        List<VehicleSeatResponse> seatResponses = seatRepository
                .findByVehicleIdOrderByFloorAscRowIndexAscColumnIndexAsc(saved.getId())
                .stream()
                .map(VehicleSeatResponse::fromEntity)
                .toList();

        return VehicleDetailResponse.of(saved, seatResponses);
    }

    @Transactional
    public void deleteVehicle(UUID id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("phương tiện", id));

        // Kiểm tra xem phương tiện có ghế không, nếu có cần thông báo hoặc xóa theo quy tắc
        long seatCount = seatRepository.countByVehicleId(id);
        if (seatCount > 0) {
            // Xóa cascade ghế nếu chưa có chuyến xe gán (sau này Trip sẽ có FK RESTRICT)
            seatRepository.deleteByVehicleId(id);
        }

        vehicleRepository.delete(vehicle);
        log.info("Đã xóa hoàn toàn phương tiện [ID: {}, Biển số: {}]", id, vehicle.getPlateNumber());
    }

    private String normalizePlateNumber(String plateNumber) {
        if (plateNumber == null) return "";
        return plateNumber.trim().toUpperCase().replaceAll("\\s+", "");
    }
}
