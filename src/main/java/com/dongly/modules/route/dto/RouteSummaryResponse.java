package com.dongly.modules.route.dto;

import com.dongly.modules.route.entity.CommonStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteSummaryResponse {
    private UUID id;
    private String code;
    private String name;
    private LocationResponse originLocation;
    private LocationResponse destinationLocation;
    private BigDecimal distanceKm;
    private Integer estimatedDurationMinutes;
    private int totalStops;
    private CommonStatus status;
    private String createdBy;
    private String updatedBy;
    private Instant createdAt;
    private Instant updatedAt;
}
