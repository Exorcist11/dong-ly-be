package com.dongly.modules.fleet.dto;

import com.dongly.modules.fleet.entity.Driver;
import com.dongly.modules.fleet.entity.DriverStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverResponse {
    private UUID id;
    private String code;
    private String fullName;
    private String phone;
    private String licenseNumber;
    private String licenseClass;
    private LocalDate licenseExpiryDate;
    private LocalDate dateOfBirth;
    private DriverStatus status;
    private String note;
    private String createdBy;
    private String updatedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static DriverResponse fromEntity(Driver driver) {
        return DriverResponse.builder()
                .id(driver.getId())
                .code(driver.getCode())
                .fullName(driver.getFullName())
                .phone(driver.getPhone())
                .licenseNumber(driver.getLicenseNumber())
                .licenseClass(driver.getLicenseClass())
                .licenseExpiryDate(driver.getLicenseExpiryDate())
                .dateOfBirth(driver.getDateOfBirth())
                .status(driver.getStatus())
                .note(driver.getNote())
                .createdBy(driver.getCreatedBy())
                .updatedBy(driver.getUpdatedBy())
                .createdAt(driver.getCreatedAt())
                .updatedAt(driver.getUpdatedAt())
                .build();
    }
}
