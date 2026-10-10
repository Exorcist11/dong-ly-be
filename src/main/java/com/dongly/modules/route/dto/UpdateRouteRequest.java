package com.dongly.modules.route.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
public class UpdateRouteRequest {

    @NotBlank(message = "Mã tuyến không được để trống")
    @Size(max = 50, message = "Mã tuyến tối đa 50 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Mã tuyến chỉ được chứa chữ cái, số, dấu gạch dưới hoặc gạch ngang")
    private String code;

    @NotBlank(message = "Tên tuyến không được để trống")
    @Size(max = 150, message = "Tên tuyến tối đa 150 ký tự")
    private String name;

    @NotNull(message = "Điểm khởi hành không được để trống")
    private UUID originLocationId;

    @NotNull(message = "Điểm đến không được để trống")
    private UUID destinationLocationId;

    @Positive(message = "Khoảng cách cự ly phải lớn hơn 0")
    private BigDecimal distanceKm;

    @Positive(message = "Thời gian di chuyển ước tính phải lớn hơn 0")
    private Integer estimatedDurationMinutes;

    private String description;
}
