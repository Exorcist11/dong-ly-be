package com.dongly.modules.trip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateTripsPreviewItem {

    private UUID tripRunId;
    private String tripRunCode;
    private String tripRunName;
    private LocalDate targetDate;
    private int dayOfWeek;
    private OffsetDateTime departureTime;
    private OffsetDateTime estimatedArrivalTime;
    private UUID routeId;
    private String routeCode;
    private String routeName;
    private String vehiclePlateNumber;
    private String driverName;
    private String assistantDriverName;
    private boolean alreadyExists;
    private String statusText;
}
