package com.dongly.modules.trip.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateTripsRequest {

    /**
     * ID của TripRun cần sinh chuyến (nếu để trống, hệ thống sẽ sinh cho tất cả các TripRun đang ACTIVE).
     */
    private UUID tripRunId;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate fromDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate toDate;
}
