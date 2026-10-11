package com.dongly.modules.booking.dto;

import com.dongly.modules.booking.entity.BookingItem;
import com.dongly.modules.booking.entity.BookingItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chi tiết ghế và vé điện tử trong đơn đặt vé")
public class BookingItemResponse {

    @Schema(description = "ID chi tiết vé")
    private UUID id;

    @Schema(description = "ID ghế")
    private UUID seatId;

    @Schema(description = "Mã số ghế", example = "A01")
    private String seatCode;

    @Schema(description = "Họ tên hành khách ngồi ghế")
    private String passengerName;

    @Schema(description = "Số điện thoại hành khách")
    private String passengerPhone;

    @Schema(description = "Tên điểm đón")
    private String pickupStopName;

    @Schema(description = "Tên điểm trả")
    private String dropoffStopName;

    @Schema(description = "Giá cơ bản của chuyến")
    private BigDecimal basePrice;

    @Schema(description = "Phụ phí loại ghế")
    private BigDecimal seatExtraPrice;

    @Schema(description = "Phụ phí điểm đón")
    private BigDecimal pickupExtraPrice;

    @Schema(description = "Phụ phí điểm trả")
    private BigDecimal dropoffExtraPrice;

    @Schema(description = "Tổng giá của ghế này")
    private BigDecimal finalPrice;

    @Schema(description = "Trạng thái của ghế")
    private BookingItemStatus status;

    public static BookingItemResponse fromEntity(BookingItem item) {
        if (item == null) return null;

        BookingItemResponseBuilder builder = BookingItemResponse.builder()
                .id(item.getId())
                .seatCode(item.getSeatCode())
                .passengerName(item.getPassengerName())
                .passengerPhone(item.getPassengerPhone())
                .basePrice(item.getBasePrice())
                .seatExtraPrice(item.getSeatExtraPrice())
                .pickupExtraPrice(item.getPickupExtraPrice())
                .dropoffExtraPrice(item.getDropoffExtraPrice())
                .finalPrice(item.getFinalPrice())
                .status(item.getStatus());

        if (item.getSeat() != null) {
            builder.seatId(item.getSeat().getId());
        }

        if (item.getPickupStop() != null && item.getPickupStop().getStopPoint() != null) {
            builder.pickupStopName(item.getPickupStop().getStopPoint().getName());
        }

        if (item.getDropoffStop() != null && item.getDropoffStop().getStopPoint() != null) {
            builder.dropoffStopName(item.getDropoffStop().getStopPoint().getName());
        }

        return builder.build();
    }
}
