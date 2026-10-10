package com.dongly.modules.trip.dto;

import com.dongly.modules.trip.entity.TripRunStatus;
import jakarta.validation.constraints.DecimalMin;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTripRunRequest {

    @NotBlank(message = "Mã lịch vòng chạy không được để trống")
    @Size(max = 50, message = "Mã lịch vòng chạy tối đa 50 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Mã lịch vòng chạy chỉ chứa chữ cái, số, gạch ngang và gạch dưới")
    private String code;

    @NotBlank(message = "Tên lịch vòng chạy không được để trống")
    @Size(max = 150, message = "Tên lịch vòng chạy tối đa 150 ký tự")
    private String name;

    @NotNull(message = "Vui lòng chọn tuyến đường")
    private UUID routeId;

    @NotNull(message = "Vui lòng chọn giờ xuất bến hàng ngày")
    private LocalTime departureTime;

    @NotBlank(message = "Vui lòng cấu hình các ngày hoạt động trong tuần")
    @Pattern(regexp = "^[1-7](,[1-7])*$", message = "Định dạng các thứ trong tuần không hợp lệ (VD: 1,2,3,4,5,6,7)")
    private String daysOfWeek;

    @NotNull(message = "Vui lòng chọn ngày bắt đầu áp dụng")
    private LocalDate startDate;

    private LocalDate endDate;

    private UUID defaultVehicleId;

    private UUID defaultDriverId;

    private UUID defaultAssistantDriverId;

    @NotNull(message = "Giá vé cơ sở không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá vé cơ sở không được âm")
    private BigDecimal basePrice;

    private TripRunStatus status;

    private String note;
}
