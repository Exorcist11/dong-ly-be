package com.dongly.modules.booking.entity;

/**
 * Kênh tiếp nhận đơn đặt vé.
 */
public enum BookingChannel {
    CRM_PHONE,   // Đặt qua cuộc gọi tổng đài / CRM
    CRM_POS,     // Đặt trực tiếp tại quầy vé bến bãi
    ONLINE       // Khách đặt trực tuyến qua Web Portal
}
