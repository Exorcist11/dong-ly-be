package com.dongly.modules.trip.dto;

import com.dongly.modules.trip.entity.TripRun;
import com.dongly.modules.trip.entity.TripRunStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripRunResponse {

    private UUID id;
    private String code;
    private String name;
    private UUID routeId;
    private String routeCode;
    private String routeName;
    private Integer estimatedDurationMinutes;
    private LocalTime departureTime;
    private String daysOfWeek;
    private LocalDate startDate;
    private LocalDate endDate;

    private UUID defaultVehicleId;
    private String defaultVehiclePlateNumber;
    private String defaultVehicleType;

    private UUID defaultDriverId;
    private String defaultDriverName;
    private String defaultDriverPhone;

    private UUID defaultAssistantDriverId;
    private String defaultAssistantDriverName;
    private String defaultAssistantDriverPhone;

    private BigDecimal basePrice;
    private TripRunStatus status;
    private String note;
    private String createdBy;
    private String updatedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static TripRunResponse fromEntity(TripRun run) {
        if (run == null) return null;

        TripRunResponse.TripRunResponseBuilder builder = TripRunResponse.builder()
                .id(run.getId())
                .code(run.getCode())
                .name(run.getName())
                .departureTime(run.getDepartureTime())
                .daysOfWeek(run.getDaysOfWeek())
                .startDate(run.getStartDate())
                .endDate(run.getEndDate())
                .basePrice(run.getBasePrice())
                .status(run.getStatus())
                .note(run.getNote())
                .createdBy(run.getCreatedBy())
                .updatedBy(run.getUpdatedBy())
                .createdAt(run.getCreatedAt())
                .updatedAt(run.getUpdatedAt());

        if (run.getRoute() != null) {
            builder.routeId(run.getRoute().getId())
                    .routeCode(run.getRoute().getCode())
                    .routeName(run.getRoute().getName())
                    .estimatedDurationMinutes(run.getRoute().getEstimatedDurationMinutes());
        }

        if (run.getDefaultVehicle() != null) {
            builder.defaultVehicleId(run.getDefaultVehicle().getId())
                    .defaultVehiclePlateNumber(run.getDefaultVehicle().getPlateNumber())
                    .defaultVehicleType(run.getDefaultVehicle().getVehicleType() != null
                            ? run.getDefaultVehicle().getVehicleType().name() : null);
        }

        if (run.getDefaultDriver() != null) {
            builder.defaultDriverId(run.getDefaultDriver().getId())
                    .defaultDriverName(run.getDefaultDriver().getFullName())
                    .defaultDriverPhone(run.getDefaultDriver().getPhone());
        }

        if (run.getDefaultAssistantDriver() != null) {
            builder.defaultAssistantDriverId(run.getDefaultAssistantDriver().getId())
                    .defaultAssistantDriverName(run.getDefaultAssistantDriver().getFullName())
                    .defaultAssistantDriverPhone(run.getDefaultAssistantDriver().getPhone());
        }

        return builder.build();
    }
}
