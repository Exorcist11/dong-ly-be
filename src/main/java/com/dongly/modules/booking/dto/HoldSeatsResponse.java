package com.dongly.modules.booking.dto;

import com.dongly.modules.booking.entity.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Phản hồi giữ ghế tạm thời thành công")
public class HoldSeatsResponse {

    @Schema(description = "ID đơn đặt chỗ")
    private UUID bookingId;

    @Schema(description = "Mã đơn đặt chỗ", example = "DL-261011-0001")
    private String bookingCode;

    @Schema(description = "ID chuyến xe")
    private UUID tripId;

    @Schema(description = "Trạng thái đơn (HELD)", example = "HELD")
    private BookingStatus status;

    @Schema(description = "Thời điểm hết hạn giữ ghế (cố định 10 phút)")
    private OffsetDateTime holdExpiresAt;

    @Schema(description = "Danh sách mã các ghế đã giữ", example = "[\"A01\", \"A02\"]")
    private List<String> heldSeats;

    @Schema(description = "Tổng tiền tạm tính (VNĐ)", example = "600000.00")
    private BigDecimal totalEstimatedAmount;

    @Schema(description = "Tên khách hàng", example = "Nguyễn Văn Nam")
    private String customerName;

    @Schema(description = "Số điện thoại khách hàng", example = "0912345678")
    private String customerPhone;
}
