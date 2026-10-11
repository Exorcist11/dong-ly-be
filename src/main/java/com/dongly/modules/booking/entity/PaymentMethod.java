package com.dongly.modules.booking.entity;

/**
 * Phương thức thanh toán được hỗ trợ.
 */
public enum PaymentMethod {
    CASH,          // Tiền mặt tại quầy
    BANK_TRANSFER, // Chuyển khoản ngân hàng thủ công
    VIETQR         // Quét mã VietQR động
}
