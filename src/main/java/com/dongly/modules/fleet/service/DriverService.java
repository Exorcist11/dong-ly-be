package com.dongly.modules.fleet.service;

import com.dongly.common.exception.AppException;
import com.dongly.common.exception.BusinessRuleException;
import com.dongly.common.exception.ErrorCode;
import com.dongly.common.exception.ResourceNotFoundException;
import com.dongly.modules.fleet.dto.CreateDriverRequest;
import com.dongly.modules.fleet.dto.DriverResponse;
import com.dongly.modules.fleet.dto.UpdateDriverRequest;
import com.dongly.modules.fleet.entity.Driver;
import com.dongly.modules.fleet.entity.DriverStatus;
import com.dongly.modules.fleet.repository.DriverRepository;
import com.dongly.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class DriverService {

    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    @Transactional(readOnly = true)
    public Page<DriverResponse> searchDrivers(
            String keyword,
            DriverStatus status,
            String licenseClass,
            Pageable pageable
    ) {
        String cleanKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String cleanClass = (licenseClass != null && !licenseClass.isBlank()) ? licenseClass.trim().toUpperCase() : null;
        return driverRepository.searchDrivers(cleanKeyword, status, cleanClass, pageable)
                .map(DriverResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public DriverResponse getDriverById(UUID id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("tài xế", id));
        return DriverResponse.fromEntity(driver);
    }

    @Transactional
    public DriverResponse createDriver(CreateDriverRequest request, CurrentUser currentUser) {
        String cleanCode = request.getCode().trim().toUpperCase();
        String cleanPhone = request.getPhone().trim();
        String cleanLicense = request.getLicenseNumber().trim();

        if (driverRepository.existsByCode(cleanCode)) {
            throw new AppException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Mã tài xế đã tồn tại: " + cleanCode);
        }
        if (driverRepository.existsByPhone(cleanPhone)) {
            throw new AppException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Số điện thoại đã được đăng ký: " + cleanPhone);
        }
        if (driverRepository.existsByLicenseNumber(cleanLicense)) {
            throw new AppException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Số giấy phép lái xe đã được đăng ký: " + cleanLicense);
        }

        String username = currentUser != null ? currentUser.username() : "SYSTEM";

        Driver driver = Driver.builder()
                .code(cleanCode)
                .fullName(request.getFullName().trim())
                .phone(cleanPhone)
                .licenseNumber(cleanLicense)
                .licenseClass(request.getLicenseClass().trim().toUpperCase())
                .licenseExpiryDate(request.getLicenseExpiryDate())
                .dateOfBirth(request.getDateOfBirth())
                .status(DriverStatus.ACTIVE)
                .note(request.getNote())
                .createdBy(username)
                .updatedBy(username)
                .build();

        Driver savedDriver = driverRepository.save(driver);
        log.info("Đã tạo mới hồ sơ tài xế [ID: {}, Code: {}] bởi {}", savedDriver.getId(), savedDriver.getCode(), username);

        return DriverResponse.fromEntity(savedDriver);
    }

    @Transactional
    public DriverResponse updateDriver(UUID id, UpdateDriverRequest request, CurrentUser currentUser) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("tài xế", id));

        String cleanCode = request.getCode().trim().toUpperCase();
        String cleanPhone = request.getPhone().trim();
        String cleanLicense = request.getLicenseNumber().trim();

        if (driverRepository.existsByCodeAndIdNot(cleanCode, id)) {
            throw new AppException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Mã tài xế đã được sử dụng bởi người khác: " + cleanCode);
        }
        if (driverRepository.existsByPhoneAndIdNot(cleanPhone, id)) {
            throw new AppException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Số điện thoại đã được đăng ký cho tài xế khác: " + cleanPhone);
        }
        if (driverRepository.existsByLicenseNumberAndIdNot(cleanLicense, id)) {
            throw new AppException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Số GPLX đã được sử dụng bởi tài xế khác: " + cleanLicense);
        }

        String username = currentUser != null ? currentUser.username() : "SYSTEM";

        driver.setCode(cleanCode);
        driver.setFullName(request.getFullName().trim());
        driver.setPhone(cleanPhone);
        driver.setLicenseNumber(cleanLicense);
        driver.setLicenseClass(request.getLicenseClass().trim().toUpperCase());
        driver.setLicenseExpiryDate(request.getLicenseExpiryDate());
        driver.setDateOfBirth(request.getDateOfBirth());
        driver.setNote(request.getNote());
        driver.setUpdatedBy(username);

        Driver updatedDriver = driverRepository.save(driver);
        log.info("Đã cập nhật hồ sơ tài xế [ID: {}, Code: {}] bởi {}", updatedDriver.getId(), updatedDriver.getCode(), username);

        return DriverResponse.fromEntity(updatedDriver);
    }

    @Transactional
    public DriverResponse updateStatus(UUID id, DriverStatus newStatus, CurrentUser currentUser) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("tài xế", id));

        String username = currentUser != null ? currentUser.username() : "SYSTEM";
        driver.setStatus(newStatus);
        driver.setUpdatedBy(username);

        Driver saved = driverRepository.save(driver);
        log.info("Đã thay đổi trạng thái tài xế [ID: {}, Code: {}] sang {} bởi {}", saved.getId(), saved.getCode(), newStatus, username);

        return DriverResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteDriver(UUID id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("tài xế", id));

        driverRepository.delete(driver);
        log.info("Đã xóa hoàn toàn hồ sơ tài xế [ID: {}, Code: {}]", id, driver.getCode());
    }
}
