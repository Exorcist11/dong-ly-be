package com.dongly.modules.fleet.service;

import com.dongly.common.exception.AppException;
import com.dongly.common.exception.BusinessRuleException;
import com.dongly.common.exception.ErrorCode;
import com.dongly.common.exception.ResourceNotFoundException;
import com.dongly.modules.fleet.dto.ConfigureSeatLayoutRequest;
import com.dongly.modules.fleet.dto.SeatItemDto;
import com.dongly.modules.fleet.dto.VehicleSeatLayoutResponse;
import com.dongly.modules.fleet.dto.VehicleSeatResponse;
import com.dongly.modules.fleet.entity.SeatStatus;
import com.dongly.modules.fleet.entity.Vehicle;
import com.dongly.modules.fleet.entity.VehicleSeat;
import com.dongly.modules.fleet.repository.VehicleRepository;
import com.dongly.modules.fleet.repository.VehicleSeatRepository;
import com.dongly.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class SeatLayoutService {

    private final VehicleRepository vehicleRepository;
    private final VehicleSeatRepository seatRepository;

    public SeatLayoutService(VehicleRepository vehicleRepository, VehicleSeatRepository seatRepository) {
        this.vehicleRepository = vehicleRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional(readOnly = true)
    public VehicleSeatLayoutResponse getSeatLayout(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("phương tiện", vehicleId));

        List<VehicleSeat> seats = seatRepository.findByVehicleIdOrderByFloorAscRowIndexAscColumnIndexAsc(vehicleId);
        return VehicleSeatLayoutResponse.of(vehicle, seats);
    }

    @Transactional
    public VehicleSeatLayoutResponse configureSeatLayout(
            UUID vehicleId,
            ConfigureSeatLayoutRequest request,
            CurrentUser currentUser
    ) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("phương tiện", vehicleId));

        // 1. Kiểm tra validation logic ma trận và danh sách ghế
        validateSeatLayoutRequest(vehicle, request);

        String username = currentUser != null ? currentUser.username() : "SYSTEM";

        // 2. Xóa cấu hình ghế cũ của phương tiện
        seatRepository.deleteByVehicleId(vehicleId);
        seatRepository.flush();

        // 3. Cập nhật kích thước lưới của xe
        vehicle.setTotalFloors(request.getTotalFloors());
        vehicle.setTotalRows(request.getTotalRows());
        vehicle.setTotalColumns(request.getTotalColumns());
        vehicle.setTotalSeats(request.getSeats().size());
        vehicle.setUpdatedBy(username);
        vehicleRepository.save(vehicle);

        // 4. Lưu danh sách ghế mới
        List<VehicleSeat> newSeats = request.getSeats().stream().map(dto -> VehicleSeat.builder()
                .vehicle(vehicle)
                .seatCode(dto.getSeatCode().trim().toUpperCase())
                .floor(dto.getFloor())
                .rowIndex(dto.getRowIndex())
                .columnIndex(dto.getColumnIndex())
                .seatType(dto.getSeatType())
                .extraPrice(dto.getExtraPrice())
                .status(dto.getStatus() != null ? dto.getStatus() : SeatStatus.ACTIVE)
                .createdBy(username)
                .updatedBy(username)
                .build()
        ).toList();

        List<VehicleSeat> savedSeats = seatRepository.saveAll(newSeats);
        log.info("Đã cấu hình thành công sơ đồ {} ghế cho xe [ID: {}, Biển số: {}] bởi {}",
                savedSeats.size(), vehicle.getId(), vehicle.getPlateNumber(), username);

        return VehicleSeatLayoutResponse.of(vehicle, savedSeats);
    }

    @Transactional
    public VehicleSeatResponse updateSeatStatus(
            UUID vehicleId,
            UUID seatId,
            SeatStatus newStatus,
            CurrentUser currentUser
    ) {
        VehicleSeat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("ghế", seatId));

        if (!seat.getVehicle().getId().equals(vehicleId)) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, "Ghế không thuộc phương tiện được chỉ định");
        }

        String username = currentUser != null ? currentUser.username() : "SYSTEM";
        seat.setStatus(newStatus);
        seat.setUpdatedBy(username);

        VehicleSeat savedSeat = seatRepository.save(seat);

        // Cập nhật lại totalSeats hoạt động của xe
        long activeCount = seatRepository.countByVehicleIdAndStatus(vehicleId, SeatStatus.ACTIVE);
        Vehicle vehicle = seat.getVehicle();
        vehicle.setTotalSeats((int) activeCount);
        vehicle.setUpdatedBy(username);
        vehicleRepository.save(vehicle);

        log.info("Đã cập nhật trạng thái ghế [ID: {}, Code: {}] sang {} bởi {}",
                savedSeat.getId(), savedSeat.getSeatCode(), newStatus, username);

        return VehicleSeatResponse.fromEntity(savedSeat);
    }

    private void validateSeatLayoutRequest(Vehicle vehicle, ConfigureSeatLayoutRequest request) {
        Set<String> seatCodes = new HashSet<>();
        Set<String> positions = new HashSet<>();

        for (SeatItemDto item : request.getSeats()) {
            String code = item.getSeatCode().trim().toUpperCase();
            if (!seatCodes.add(code)) {
                throw new BusinessRuleException("Mã ghế bị trùng lặp trong sơ đồ: " + code);
            }

            if (item.getFloor() > request.getTotalFloors() || item.getFloor() < 1) {
                throw new BusinessRuleException(
                        String.format("Ghế %s có tầng %d vượt quá số tầng cấu hình (%d)",
                                code, item.getFloor(), request.getTotalFloors()));
            }

            if (item.getRowIndex() > request.getTotalRows() || item.getRowIndex() < 1) {
                throw new BusinessRuleException(
                        String.format("Ghế %s có hàng %d vượt quá số hàng cấu hình (%d)",
                                code, item.getRowIndex(), request.getTotalRows()));
            }

            if (item.getColumnIndex() > request.getTotalColumns() || item.getColumnIndex() < 1) {
                throw new BusinessRuleException(
                        String.format("Ghế %s có cột %d vượt quá số cột cấu hình (%d)",
                                code, item.getColumnIndex(), request.getTotalColumns()));
            }

            String posKey = String.format("%d_%d_%d", item.getFloor(), item.getRowIndex(), item.getColumnIndex());
            if (!positions.add(posKey)) {
                throw new BusinessRuleException(
                        String.format("Trùng lặp tọa độ vật lý (Tầng %d, Hàng %d, Cột %d) cho nhiều ghế khác nhau!",
                                item.getFloor(), item.getRowIndex(), item.getColumnIndex()));
            }
        }
    }
}
