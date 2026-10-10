package com.dongly.modules.trip.dto;

import com.dongly.modules.trip.entity.TripStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTripStatusRequest {

    @NotNull(message = "Trạng thái mới không được để trống")
    private TripStatus status;

    private OffsetDateTime actualDepartureTime;

    private OffsetDateTime actualArrivalTime;

    private String note;
}
