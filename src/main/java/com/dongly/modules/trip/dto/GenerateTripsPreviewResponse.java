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
public class GenerateTripsPreviewResponse {

    private int totalDatesChecked;
    private int willCreateCount;
    private int alreadyExistsCount;

    @Builder.Default
    private List<GenerateTripsPreviewItem> items = new ArrayList<>();
}
