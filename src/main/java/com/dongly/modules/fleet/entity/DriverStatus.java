package com.dongly.modules.fleet.entity;

/**
 * Định nghĩa trạng thái làm việc của tài xế
 */
public enum DriverStatus {
    ACTIVE,     // Đang sẵn sàng nhận chuyến
    ON_LEAVE,   // Đang nghỉ phép / tạm hoãn
    INACTIVE    // Đã nghỉ việc / ngừng hợp tác
}
