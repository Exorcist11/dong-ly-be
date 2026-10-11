package com.dongly.modules.booking.dto;

import com.dongly.modules.booking.entity.SeatOccupancyStatus;
import com.dongly.modules.fleet.entity.SeatType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chi tiết ghế trên sơ đồ của chuyến xe")
public class CrmTripSeatItemDto {

    @Schema(description = "ID ghế vật lý")
    private UUID seatId;

    @Schema(description = "Mã số ghế", example = "A01")
    private String seatCode;

    @Schema(description = "Tầng (1 hoặc 2)", example = "1")
    private int floor;

    @Schema(description = "Chỉ số hàng", example = "1")
    private int rowIndex;

    @Schema(description = "Chỉ số cột", example = "1")
    private int columnIndex;

    @Schema(description = "Loại ghế", example = "LUXURY_ROOM")
    private SeatType seatType;

    @Schema(description = "Phụ phí loại ghế (VNĐ)", example = "50000.00")
    private BigDecimal seatExtraPrice;

    @Schema(description = "Giá vé dự kiến của riêng ghế này trên chuyến (basePrice + seatExtraPrice)", example = "300000.00")
    private BigDecimal calculatedPrice;

    @Schema(description = "Trạng thái đặt chỗ thời gian thực (AVAILABLE, HELD, BOOKED, LOCKED)", example = "AVAILABLE")
    private SeatOccupancyStatus occupancyStatus;

    @Schema(description = "Thời điểm hết hạn giữ chỗ (nếu đang ở trạng thái HELD)")
    private OffsetDateTime holdExpiresAt;

    @Schema(description = "Cờ đánh dấu có thể chọn đặt ngay tại thời điểm này không (chỉ true khi AVAILABLE)", example = "true")
    private boolean isBookable;
}
