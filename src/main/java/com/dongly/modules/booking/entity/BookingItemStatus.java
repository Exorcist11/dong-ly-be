package com.dongly.modules.booking.entity;

/**
 * Trạng thái của từng ghế / hành khách trong đơn đặt vé (BookingItem).
 */
public enum BookingItemStatus {
    HELD,
    CONFIRMED,
    CANCELLED,
    EXPIRED
}
