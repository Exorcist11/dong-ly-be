package com.dongly.modules.fleet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigureSeatLayoutRequest {

    @Schema(description = "Số tầng xe (1 hoặc 2)", example = "2")
    @NotNull(message = "Số tầng xe không được để trống")
    @Min(value = 1, message = "Số tầng xe tối thiểu là 1")
    @Max(value = 2, message = "Số tầng xe tối đa là 2")
    private Integer totalFloors;

    @Schema(description = "Số hàng ghế tối đa của sơ đồ", example = "6")
    @NotNull(message = "Số hàng ghế không được để trống")
    @Min(value = 1, message = "Số hàng ghế tối thiểu là 1")
    @Max(value = 20, message = "Số hàng ghế tối đa là 20")
    private Integer totalRows;

    @Schema(description = "Số cột ghế tối đa của sơ đồ", example = "3")
    @NotNull(message = "Số cột ghế không được để trống")
    @Min(value = 1, message = "Số cột ghế tối thiểu là 1")
    @Max(value = 10, message = "Số cột ghế tối đa là 10")
    private Integer totalColumns;

    @Schema(description = "Danh sách chi tiết các ghế cấu hình trên sơ đồ")
    @NotEmpty(message = "Danh sách ghế không được để trống")
    @Valid
    private List<SeatItemDto> seats;
}
