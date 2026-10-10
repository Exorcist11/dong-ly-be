package com.dongly.modules.fleet.repository;

import com.dongly.modules.fleet.entity.Vehicle;
import com.dongly.modules.fleet.entity.VehicleStatus;
import com.dongly.modules.fleet.entity.VehicleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    Optional<Vehicle> findByPlateNumber(String plateNumber);

    boolean existsByPlateNumber(String plateNumber);

    boolean existsByPlateNumberAndIdNot(String plateNumber, UUID id);

    @Query("""
        SELECT v FROM Vehicle v
        WHERE (:keyword IS NULL OR LOWER(v.plateNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(v.brand) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(v.model) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:vehicleType IS NULL OR v.vehicleType = :vehicleType)
          AND (:status IS NULL OR v.status = :status)
    """)
    Page<Vehicle> searchVehicles(
            @Param("keyword") String keyword,
            @Param("vehicleType") VehicleType vehicleType,
            @Param("status") VehicleStatus status,
            Pageable pageable
    );
}
