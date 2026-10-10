package com.dongly.modules.route.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRouteStopsRequest {

    @NotNull(message = "Danh sách điểm dừng không được là null")
    @Valid
    private List<RouteStopInputDto> stops;
}
