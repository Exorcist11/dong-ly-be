package com.dongly.modules.fleet.repository;

import com.dongly.modules.fleet.entity.Driver;
import com.dongly.modules.fleet.entity.DriverStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverRepository extends JpaRepository<Driver, UUID> {

    Optional<Driver> findByCode(String code);

    Optional<Driver> findByPhone(String phone);

    Optional<Driver> findByLicenseNumber(String licenseNumber);

    boolean existsByCode(String code);

    boolean existsByPhone(String phone);

    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByCodeAndIdNot(String code, UUID id);

    boolean existsByPhoneAndIdNot(String phone, UUID id);

    boolean existsByLicenseNumberAndIdNot(String licenseNumber, UUID id);

    @Query("""
        SELECT d FROM Driver d
        WHERE (
            :keyword IS NULL OR :keyword = ''
            OR LOWER(d.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(d.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(d.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(d.licenseNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
          AND (:status IS NULL OR d.status = :status)
          AND (:licenseClass IS NULL OR :licenseClass = '' OR d.licenseClass = :licenseClass)
    """)
    Page<Driver> searchDrivers(
            @Param("keyword") String keyword,
            @Param("status") DriverStatus status,
            @Param("licenseClass") String licenseClass,
            Pageable pageable
    );
}
