package com.dongly.modules.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tiêu chí tìm kiếm chuyến xe cho CRM Booking")
public class CrmTripSearchCriteria {

    @Schema(description = "ID tuyến đường (Tùy chọn)")
    private UUID routeId;

    @Schema(description = "ID địa phương xuất phát (Tùy chọn)")
    private UUID originLocationId;

    @Schema(description = "ID địa phương đích đến (Tùy chọn)")
    private UUID destinationLocationId;

    @NotNull(message = "Ngày khởi hành không được để trống")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Ngày khởi hành (YYYY-MM-DD)", example = "2026-10-15", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate departureDate;

    @Schema(description = "Từ khóa tìm kiếm (mã chuyến, biển số xe, tên tuyến)")
    private String keyword;

    @Min(value = 0, message = "Số trang phải từ 0 trở lên")
    @Builder.Default
    @Schema(description = "Số trang (bắt đầu từ 0)", example = "0")
    private int page = 0;

    @Min(value = 1, message = "Kích thước trang phải từ 1")
    @Max(value = 100, message = "Kích thước trang tối đa là 100")
    @Builder.Default
    @Schema(description = "Số lượng bản ghi mỗi trang (tối đa 100)", example = "20")
    private int size = 20;

    @Builder.Default
    @Schema(description = "Sắp xếp theo trường và chiều (ví dụ: departureTime,asc)", example = "departureTime,asc")
    private String sort = "departureTime,asc";
}
