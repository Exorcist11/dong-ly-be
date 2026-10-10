package com.dongly.modules.route.dto;

import com.dongly.modules.route.entity.CommonStatus;
import com.dongly.modules.route.entity.RouteDirectionType;
import com.dongly.modules.route.entity.RouteStopType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteStopResponse {
    private UUID id;
    private UUID stopPointId;
    private String stopPointCode;
    private String stopPointName;
    private String address;
    private String locationName;
    private RouteDirectionType direction;
    private Integer sequence;
    private RouteStopType stopType;
    private BigDecimal extraPrice;
    private CommonStatus status;
}
