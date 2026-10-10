package com.dongly.modules.route.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class CreateStopPointRequest {

    @NotBlank(message = "Mã điểm đón/trả không được để trống")
    @Size(max = 50, message = "Mã điểm đón/trả tối đa 50 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Mã điểm đón/trả chỉ được chứa chữ cái, số, dấu gạch dưới hoặc gạch ngang")
    private String code;

    @NotBlank(message = "Tên điểm đón/trả không được để trống")
    @Size(max = 150, message = "Tên điểm đón/trả tối đa 150 ký tự")
    private String name;

    @NotNull(message = "Địa phương trực thuộc không được để trống")
    private UUID locationId;

    @NotBlank(message = "Địa chỉ cụ thể không được để trống")
    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Size(max = 20, message = "Số điện thoại liên hệ tối đa 20 ký tự")
    private String contactPhone;
}
