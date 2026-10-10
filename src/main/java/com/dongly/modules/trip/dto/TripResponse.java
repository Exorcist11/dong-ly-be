package com.dongly.modules.trip.dto;

import com.dongly.modules.trip.entity.Trip;
import com.dongly.modules.trip.entity.TripStatus;
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
public class TripResponse {

    private UUID id;
    private String code;
    private UUID tripRunId;
    private String tripRunCode;

    // Route info
    private UUID routeId;
    private String routeCode;
    private String routeName;
    private BigDecimal distanceKm;
    private Integer estimatedDurationMinutes;

    // Vehicle info
    private UUID vehicleId;
    private String vehiclePlateNumber;
    private String vehicleType;
    private Integer totalSeats;

    // Drivers info
    private UUID driverId;
    private String driverName;
    private String driverPhone;

    private UUID assistantDriverId;
    private String assistantDriverName;
    private String assistantDriverPhone;

    // Time & Price
    private OffsetDateTime departureTime;
    private OffsetDateTime estimatedArrivalTime;
    private OffsetDateTime actualDepartureTime;
    private OffsetDateTime actualArrivalTime;
    private BigDecimal basePrice;
    private TripStatus status;
    private String note;

    // Audit
    private String createdBy;
    private String updatedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static TripResponse fromEntity(Trip trip) {
        if (trip == null) return null;

        TripResponseBuilder builder = TripResponse.builder()
                .id(trip.getId())
                .code(trip.getCode())
                .departureTime(trip.getDepartureTime())
                .estimatedArrivalTime(trip.getEstimatedArrivalTime())
                .actualDepartureTime(trip.getActualDepartureTime())
                .actualArrivalTime(trip.getActualArrivalTime())
                .basePrice(trip.getBasePrice())
                .status(trip.getStatus())
                .note(trip.getNote())
                .createdBy(trip.getCreatedBy())
                .updatedBy(trip.getUpdatedBy())
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt());

        if (trip.getTripRun() != null) {
            builder.tripRunId(trip.getTripRun().getId())
                    .tripRunCode(trip.getTripRun().getCode());
        }

        if (trip.getRoute() != null) {
            builder.routeId(trip.getRoute().getId())
                    .routeCode(trip.getRoute().getCode())
                    .routeName(trip.getRoute().getName())
                    .distanceKm(trip.getRoute().getDistanceKm())
                    .estimatedDurationMinutes(trip.getRoute().getEstimatedDurationMinutes());
        }

        if (trip.getVehicle() != null) {
            builder.vehicleId(trip.getVehicle().getId())
                    .vehiclePlateNumber(trip.getVehicle().getPlateNumber())
                    .vehicleType(trip.getVehicle().getVehicleType() != null
                            ? trip.getVehicle().getVehicleType().name() : null)
                    .totalSeats(trip.getVehicle().getTotalSeats());
        }

        if (trip.getDriver() != null) {
            builder.driverId(trip.getDriver().getId())
                    .driverName(trip.getDriver().getFullName())
                    .driverPhone(trip.getDriver().getPhone());
        }

        if (trip.getAssistantDriver() != null) {
            builder.assistantDriverId(trip.getAssistantDriver().getId())
                    .assistantDriverName(trip.getAssistantDriver().getFullName())
                    .assistantDriverPhone(trip.getAssistantDriver().getPhone());
        }

        return builder.build();
    }
}
