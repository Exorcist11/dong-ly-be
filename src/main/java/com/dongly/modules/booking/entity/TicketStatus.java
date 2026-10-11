package com.dongly.modules.booking.entity;

/**
 * Trạng thái của vé điện tử (Ticket).
 */
public enum TicketStatus {
    ISSUED,      // Đã phát hành vé
    CHECKED_IN,  // Hành khách đã check-in lên xe
    CANCELLED    // Vé đã bị hủy
}
