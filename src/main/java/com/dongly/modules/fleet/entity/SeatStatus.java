package com.dongly.modules.fleet.entity;

/**
 * Định nghĩa trạng thái ghế
 */
public enum SeatStatus {
    ACTIVE,    // Ghế đang mở bán / sử dụng
    BLOCKED,   // Ghế tạm khóa (dành cho phụ xe, trưởng xe, bảo trì)
    INACTIVE   // Không sử dụng
}
