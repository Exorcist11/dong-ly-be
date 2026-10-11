package com.dongly.modules.booking.entity;

/**
 * Trạng thái giao dịch thanh toán.
 */
public enum PaymentStatus {
    PAID,       // Đã thanh toán thành công
    CANCELLED,  // Giao dịch bị hủy
    REFUNDED    // Đã hoàn tiền cho khách
}
