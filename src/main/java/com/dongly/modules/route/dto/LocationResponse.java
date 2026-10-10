package com.dongly.modules.route.dto;

import com.dongly.modules.route.entity.CommonStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationResponse {
    private UUID id;
    private String code;
    private String name;
    private String province;
    private CommonStatus status;
}
