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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sơ đồ ghế thời gian thực và thông tin chuyến xe cho CRM Booking")
public class CrmTripSeatMapResponse {

    @Schema(description = "ID chuyến xe")
    private UUID tripId;

    @Schema(description = "Mã chuyến xe", example = "TRP-20261015-0400-36B02868")
    private String tripCode;

    // Route info
    @Schema(description = "ID tuyến đường")
    private UUID routeId;

    @Schema(description = "Tên tuyến đường", example = "Thanh Hóa - Hà Nội")
    private String routeName;

    // Time & Price
    @Schema(description = "Thời gian xuất bến")
    private OffsetDateTime departureTime;

    @Schema(description = "Thời gian đến dự kiến")
    private OffsetDateTime estimatedArrivalTime;

    @Schema(description = "Giá vé cơ bản khởi điểm (VNĐ)", example = "250000.00")
    private BigDecimal basePrice;

    @Schema(description = "Trạng thái vận hành chuyến", example = "READY")
    private TripStatus status;

    // Vehicle layout
    @Schema(description = "ID phương tiện")
    private UUID vehicleId;

    @Schema(description = "Biển số xe", example = "36B-028.68")
    private String vehiclePlateNumber;

    @Schema(description = "Loại xe", example = "LIMOUSINE")
    private String vehicleType;

    @Schema(description = "Tổng số tầng (1 hoặc 2)", example = "2")
    private int totalFloors;

    @Schema(description = "Tổng số hàng", example = "6")
    private int totalRows;

    @Schema(description = "Tổng số cột", example = "3")
    private int totalColumns;

    @Schema(description = "Tổng số ghế vật lý", example = "22")
    private int totalSeats;

    @Schema(description = "Số ghế khả dụng để đặt", example = "18")
    private int availableSeats;

    @Schema(description = "Số ghế đang giữ tạm thời", example = "2")
    private int heldSeats;

    @Schema(description = "Số ghế đã đặt", example = "2")
    private int bookedSeats;

    @Schema(description = "Danh sách chi tiết ghế trên sơ đồ xe")
    @Builder.Default
    private List<CrmTripSeatItemDto> seats = new ArrayList<>();

    @Schema(description = "Danh sách điểm đón/trả hợp lệ trên hành trình của chuyến")
    @Builder.Default
    private List<CrmTripStopDto> stops = new ArrayList<>();

    public static CrmTripSeatMapResponse of(
            Trip trip,
            List<CrmTripSeatItemDto> seats,
            List<CrmTripStopDto> stops,
            int availableCount,
            int heldCount,
            int bookedCount
    ) {
        if (trip == null) return null;

        CrmTripSeatMapResponseBuilder builder = CrmTripSeatMapResponse.builder()
                .tripId(trip.getId())
                .tripCode(trip.getCode())
                .departureTime(trip.getDepartureTime())
                .estimatedArrivalTime(trip.getEstimatedArrivalTime())
                .basePrice(trip.getBasePrice())
                .status(trip.getStatus())
                .seats(seats != null ? seats : new ArrayList<>())
                .stops(stops != null ? stops : new ArrayList<>())
                .availableSeats(availableCount)
                .heldSeats(heldCount)
                .bookedSeats(bookedCount);

        if (trip.getRoute() != null) {
            builder.routeId(trip.getRoute().getId())
                    .routeName(trip.getRoute().getName());
        }

        if (trip.getVehicle() != null) {
            builder.vehicleId(trip.getVehicle().getId())
                    .vehiclePlateNumber(trip.getVehicle().getPlateNumber())
                    .vehicleType(trip.getVehicle().getVehicleType() != null
                            ? trip.getVehicle().getVehicleType().name() : null)
                    .totalFloors(trip.getVehicle().getTotalFloors() != null ? trip.getVehicle().getTotalFloors() : 1)
                    .totalRows(trip.getVehicle().getTotalRows() != null ? trip.getVehicle().getTotalRows() : 6)
                    .totalColumns(trip.getVehicle().getTotalColumns() != null ? trip.getVehicle().getTotalColumns() : 3)
                    .totalSeats(trip.getVehicle().getTotalSeats() != null ? trip.getVehicle().getTotalSeats() : 0);
        }

        return builder.build();
    }
}
