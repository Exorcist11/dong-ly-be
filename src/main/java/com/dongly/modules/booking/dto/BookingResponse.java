package com.dongly.modules.booking.dto;

import com.dongly.modules.booking.entity.Booking;
import com.dongly.modules.booking.entity.BookingChannel;
import com.dongly.modules.booking.entity.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết đơn đặt vé hoàn chỉnh")
public class BookingResponse {

    @Schema(description = "ID đơn đặt vé")
    private UUID id;

    @Schema(description = "Mã đơn đặt vé", example = "DL-261011-0001")
    private String bookingCode;

    @Schema(description = "ID chuyến xe")
    private UUID tripId;

    @Schema(description = "Mã chuyến xe", example = "TRP-20261015-0400-36B02868")
    private String tripCode;

    @Schema(description = "Tên tuyến đường", example = "Thanh Hóa - Hà Nội")
    private String routeName;

    @Schema(description = "Biển số xe", example = "36B-028.68")
    private String vehiclePlateNumber;

    @Schema(description = "Thời gian xuất bến")
    private OffsetDateTime departureTime;

    // Customer info
    @Schema(description = "ID khách hàng")
    private UUID customerId;

    @Schema(description = "Họ tên người đặt")
    private String customerName;

    @Schema(description = "Số điện thoại người đặt")
    private String customerPhone;

    @Schema(description = "Email người đặt")
    private String customerEmail;

    // Status & Financial
    @Schema(description = "Kênh đặt vé")
    private BookingChannel channel;

    @Schema(description = "Trạng thái đơn đặt vé")
    private BookingStatus status;

    @Schema(description = "Tổng số tiền thanh toán (VNĐ)")
    private BigDecimal totalAmount;

    @Schema(description = "Thời hạn giữ ghế")
    private OffsetDateTime holdExpiresAt;

    @Schema(description = "Ghi chú đơn vé")
    private String note;

    @Schema(description = "Nhân viên tạo đơn")
    private String createdBy;

    @Schema(description = "Thời điểm tạo đơn")
    private OffsetDateTime createdAt;

    @Schema(description = "Danh sách chi tiết vé / ghế")
    @Builder.Default
    private List<BookingItemResponse> items = new ArrayList<>();

    public static BookingResponse fromEntity(Booking booking) {
        if (booking == null) return null;

        BookingResponseBuilder builder = BookingResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .channel(booking.getChannel())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .holdExpiresAt(booking.getHoldExpiresAt())
                .note(booking.getNote())
                .createdBy(booking.getCreatedBy())
                .createdAt(booking.getCreatedAt());

        if (booking.getTrip() != null) {
            builder.tripId(booking.getTrip().getId())
                    .tripCode(booking.getTrip().getCode())
                    .departureTime(booking.getTrip().getDepartureTime());

            if (booking.getTrip().getRoute() != null) {
                builder.routeName(booking.getTrip().getRoute().getName());
            }
            if (booking.getTrip().getVehicle() != null) {
                builder.vehiclePlateNumber(booking.getTrip().getVehicle().getPlateNumber());
            }
        }

        if (booking.getCustomer() != null) {
            builder.customerId(booking.getCustomer().getId())
                    .customerName(booking.getCustomer().getFullName())
                    .customerPhone(booking.getCustomer().getPhone())
                    .customerEmail(booking.getCustomer().getEmail());
        }

        if (booking.getItems() != null) {
            builder.items(booking.getItems().stream().map(BookingItemResponse::fromEntity).toList());
        }

        return builder.build();
    }
}
