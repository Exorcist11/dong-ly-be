package com.dongly.modules.fleet.dto;

import com.dongly.modules.fleet.entity.SeatStatus;
import com.dongly.modules.fleet.entity.SeatType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatItemDto {

    @Schema(description = "Mã vị trí ghế (VD: A01, A02, B01...)", example = "A01")
    @NotBlank(message = "Mã ghế không được để trống")
    @Size(max = 20, message = "Mã ghế không vượt quá 20 ký tự")
    private String seatCode;

    @Schema(description = "Tầng ghế (1 hoặc 2)", example = "1")
    @NotNull(message = "Tầng ghế không được để trống")
    @Min(value = 1, message = "Tầng tối thiểu là 1")
    @Max(value = 2, message = "Tầng tối đa là 2")
    private Integer floor;

    @Schema(description = "Tọa độ hàng (1-indexed)", example = "1")
    @NotNull(message = "Tọa độ hàng không được để trống")
    @Min(value = 1, message = "Tọa độ hàng tối thiểu là 1")
    private Integer rowIndex;

    @Schema(description = "Tọa độ cột (1-indexed)", example = "1")
    @NotNull(message = "Tọa độ cột không được để trống")
    @Min(value = 1, message = "Tọa độ cột tối thiểu là 1")
    private Integer columnIndex;

    @Schema(description = "Loại ghế", example = "LUXURY_ROOM")
    @NotNull(message = "Loại ghế không được để trống")
    private SeatType seatType;

    @Schema(description = "Giá phụ thu của ghế", example = "50000")
    @DecimalMin(value = "0.0", message = "Phụ thu không được âm")
    @Builder.Default
    private BigDecimal extraPrice = BigDecimal.ZERO;

    @Schema(description = "Trạng thái ghế", example = "ACTIVE")
    @Builder.Default
    private SeatStatus status = SeatStatus.ACTIVE;
}
