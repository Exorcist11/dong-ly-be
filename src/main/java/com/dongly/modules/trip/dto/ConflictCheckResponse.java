package com.dongly.modules.trip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictCheckResponse {

    private boolean hasConflict;
    private boolean vehicleConflict;
    private boolean driverConflict;
    private boolean assistantDriverConflict;

    @Builder.Default
    private List<String> conflictMessages = new ArrayList<>();

    public static ConflictCheckResponse ok() {
        return ConflictCheckResponse.builder()
                .hasConflict(false)
                .vehicleConflict(false)
                .driverConflict(false)
                .assistantDriverConflict(false)
                .conflictMessages(new ArrayList<>())
                .build();
    }
}
