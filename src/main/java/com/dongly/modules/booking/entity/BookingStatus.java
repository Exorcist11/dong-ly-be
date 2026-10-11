package com.dongly.modules.booking.entity;

/**
 * Trạng thái của Đơn đặt vé (Booking).
 */
public enum BookingStatus {
    /**
     * Ghế đang được giữ tạm thời (cố định 10 phút)
     */
    HELD,

    /**
     * Đã thanh toán và xác nhận đặt vé thành công
     */
    CONFIRMED,

    /**
     * Đã hủy đặt vé
     */
    CANCELLED,

    /**
     * Hết thời hạn giữ chỗ 10 phút mà chưa thanh toán
     */
    EXPIRED,

    /**
     * Chuyến xe đã kết thúc an toàn
     */
    COMPLETED
}
