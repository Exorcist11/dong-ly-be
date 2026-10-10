package com.dongly.modules.trip.dto;

import com.dongly.modules.trip.entity.TripRunStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTripRunStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private TripRunStatus status;
}
