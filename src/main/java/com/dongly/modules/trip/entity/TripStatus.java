package com.dongly.modules.trip.entity;

/**
 * Vòng đời trạng thái của Chuyến xe vận hành thực tế (Trip).
 */
public enum TripStatus {
    /**
     * Chuyến xe đã được lên lịch (từ TripRun hoặc tạo thủ công).
     */
    SCHEDULED,

    /**
     * Chuyến xe đã sẵn sàng (đủ phương tiện, tài xế chính, phụ xe hợp lệ, mở bán vé).
     */
    READY,

    /**
     * Chuyến xe đã xuất bến và đang vận hành trên tuyến đường.
     */
    DEPARTED,

    /**
     * Chuyến xe đã đến bến cuối và hoàn thành an toàn.
     */
    COMPLETED,

    /**
     * Chuyến xe đã bị hủy bỏ.
     */
    CANCELLED
}
