package com.dongly.modules.booking.entity;

/**
 * Trạng thái chiếm chỗ của một vị trí ghế trong một Chuyến xe cụ thể.
 * Được tính toán động tại Backend.
 */
public enum SeatOccupancyStatus {
    /**
     * Ghế trống, khả dụng để đặt
     */
    AVAILABLE,

    /**
     * Ghế đang được giữ tạm thời (trong thời hạn 10 phút)
     */
    HELD,

    /**
     * Ghế đã được đặt và thanh toán thành công
     */
    BOOKED,

    /**
     * Ghế không khả dụng (bảo trì hoặc tạm khóa trên sơ đồ xe)
     */
    LOCKED
}
