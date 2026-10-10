package com.dongly.modules.fleet.repository;

import com.dongly.modules.fleet.entity.VehicleSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleSeatRepository extends JpaRepository<VehicleSeat, UUID> {

    List<VehicleSeat> findByVehicleIdOrderByFloorAscRowIndexAscColumnIndexAsc(UUID vehicleId);

    Optional<VehicleSeat> findByVehicleIdAndSeatCode(UUID vehicleId, String seatCode);

    boolean existsByVehicleIdAndSeatCode(UUID vehicleId, String seatCode);

    boolean existsByVehicleIdAndFloorAndRowIndexAndColumnIndex(UUID vehicleId, Integer floor, Integer rowIndex, Integer columnIndex);

    long countByVehicleId(UUID vehicleId);

    long countByVehicleIdAndStatus(UUID vehicleId, com.dongly.modules.fleet.entity.SeatStatus status);

    @Modifying
    @Query("DELETE FROM VehicleSeat s WHERE s.vehicle.id = :vehicleId")
    void deleteByVehicleId(@Param("vehicleId") UUID vehicleId);
}
