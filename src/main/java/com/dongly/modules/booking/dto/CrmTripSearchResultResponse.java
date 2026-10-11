package com.dongly.modules.booking.dto;

import com.dongly.modules.trip.entity.Trip;
import com.dongly.modules.trip.entity.TripStatus;
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
@Schema(description = "Kết quả tìm kiếm chuyến xe cho nhân viên CRM Booking")
public class CrmTripSearchResultResponse {

    @Schema(description = "ID chuyến xe")
    private UUID tripId;

    @Schema(description = "Mã chuyến xe", example = "TRP-20261015-0400-36B02868")
    private String tripCode;

    // Route info
    @Schema(description = "ID tuyến đường")
    private UUID routeId;

    @Schema(description = "Mã tuyến đường", example = "TH_HN")
    private String routeCode;

    @Schema(description = "Tên tuyến đường", example = "Thanh Hóa - Hà Nội")
    private String routeName;

    @Schema(description = "Địa phương xuất phát")
    private String originLocationName;

    @Schema(description = "Địa phương đến")
    private String destinationLocationName;

    @Schema(description = "Khoảng cách (km)", example = "160.00")
    private BigDecimal distanceKm;

    @Schema(description = "Thời gian di chuyển dự kiến (phút)", example = "180")
    private Integer estimatedDurationMinutes;

    // Vehicle info
    @Schema(description = "ID phương tiện")
    private UUID vehicleId;

    @Schema(description = "Biển số xe", example = "36B-028.68")
    private String vehiclePlateNumber;

    @Schema(description = "Loại xe", example = "LIMOUSINE")
    private String vehicleType;

    @Schema(description = "Tổng số ghế vật lý", example = "22")
    private int totalSeats;

    @Schema(description = "Số ghế khả dụng (trống và có thể đặt)", example = "18")
    private int availableSeats;

    @Schema(description = "Số ghế đang được giữ tạm thời (trong 10p)", example = "2")
    private int heldSeats;

    @Schema(description = "Số ghế đã bán thành công", example = "2")
    private int bookedSeats;

    // Time & Price
    @Schema(description = "Thời gian xuất bến")
    private OffsetDateTime departureTime;

    @Schema(description = "Thời gian đến dự kiến")
    private OffsetDateTime estimatedArrivalTime;

    @Schema(description = "Giá vé cơ bản khởi điểm (VNĐ)", example = "250000.00")
    private BigDecimal basePrice;

    @Schema(description = "Trạng thái vận hành của chuyến", example = "READY")
    private TripStatus status;

    public static CrmTripSearchResultResponse fromTripAndSeatCounts(
            Trip trip,
            int totalSeats,
            int availableSeats,
            int heldSeats,
            int bookedSeats
    ) {
        if (trip == null) return null;

        CrmTripSearchResultResponseBuilder builder = CrmTripSearchResultResponse.builder()
                .tripId(trip.getId())
                .tripCode(trip.getCode())
                .departureTime(trip.getDepartureTime())
                .estimatedArrivalTime(trip.getEstimatedArrivalTime())
                .basePrice(trip.getBasePrice())
                .status(trip.getStatus())
                .totalSeats(totalSeats)
                .availableSeats(availableSeats)
                .heldSeats(heldSeats)
                .bookedSeats(bookedSeats);

        if (trip.getRoute() != null) {
            builder.routeId(trip.getRoute().getId())
                    .routeCode(trip.getRoute().getCode())
                    .routeName(trip.getRoute().getName())
                    .distanceKm(trip.getRoute().getDistanceKm())
                    .estimatedDurationMinutes(trip.getRoute().getEstimatedDurationMinutes());

            if (trip.getRoute().getOriginLocation() != null) {
                builder.originLocationName(trip.getRoute().getOriginLocation().getName());
            }
            if (trip.getRoute().getDestinationLocation() != null) {
                builder.destinationLocationName(trip.getRoute().getDestinationLocation().getName());
            }
        }

        if (trip.getVehicle() != null) {
            builder.vehicleId(trip.getVehicle().getId())
                    .vehiclePlateNumber(trip.getVehicle().getPlateNumber())
                    .vehicleType(trip.getVehicle().getVehicleType() != null
                            ? trip.getVehicle().getVehicleType().name() : null);
        }

        return builder.build();
    }
}
