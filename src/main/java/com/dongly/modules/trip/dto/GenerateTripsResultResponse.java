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
public class GenerateTripsResultResponse {

    private int totalDatesChecked;
    private int createdCount;
    private int skippedCount;
    private String message;

    @Builder.Default
    private List<String> createdTripCodes = new ArrayList<>();
}
