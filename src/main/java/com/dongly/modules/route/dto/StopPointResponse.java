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
public class StopPointResponse {
    private UUID id;
    private String code;
    private String name;
    private UUID locationId;
    private String locationName;
    private String locationProvince;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String contactPhone;
    private CommonStatus status;
    private String createdBy;
    private String updatedBy;
    private Instant createdAt;
    private Instant updatedAt;
}
