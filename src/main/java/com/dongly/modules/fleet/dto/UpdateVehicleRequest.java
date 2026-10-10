package com.dongly.modules.fleet.dto;

import com.dongly.modules.fleet.entity.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVehicleRequest {

    @Schema(description = "Biển số xe (VD: 36B-028.68)", example = "36B-028.68")
    @NotBlank(message = "Biển số xe không được để trống")
    @Size(max = 20, message = "Biển số xe không vượt quá 20 ký tự")
    @Pattern(regexp = "^[0-9]{2}[A-Z]-[0-9]{3,5}(\\.[0-9]{2})?$", message = "Biển số xe không đúng định dạng (VD: 36B-123.45 hoặc 29B-12345)")
    private String plateNumber;

    @Schema(description = "Loại phương tiện", example = "LIMOUSINE")
    @NotNull(message = "Loại phương tiện không được để trống")
    private VehicleType vehicleType;

    @Schema(description = "Hãng sản xuất / Thương hiệu", example = "Thaco Mobihome")
    @NotBlank(message = "Thương hiệu không được để trống")
    @Size(max = 100, message = "Thương hiệu không vượt quá 100 ký tự")
    private String brand;

    @Schema(description = "Dòng xe / Model", example = "VIP 22 Phòng")
    @Size(max = 100, message = "Dòng xe không vượt quá 100 ký tự")
    private String model;

    @Schema(description = "Năm sản xuất", example = "2024")
    @Min(value = 1990, message = "Năm sản xuất phải từ 1990 trở lên")
    @Max(value = 2100, message = "Năm sản xuất không hợp lệ")
    private Integer manufactureYear;

    @Schema(description = "Số tầng xe (1 hoặc 2)", example = "2")
    @NotNull(message = "Số tầng xe không được để trống")
    @Min(value = 1, message = "Số tầng xe tối thiểu là 1")
    @Max(value = 2, message = "Số tầng xe tối đa là 2")
    private Integer totalFloors;

    @Schema(description = "Số hàng ghế tối đa trên sơ đồ", example = "6")
    @NotNull(message = "Số hàng ghế không được để trống")
    @Min(value = 1, message = "Số hàng ghế tối thiểu là 1")
    @Max(value = 20, message = "Số hàng ghế tối đa là 20")
    private Integer totalRows;

    @Schema(description = "Số cột ghế tối đa trên sơ đồ", example = "3")
    @NotNull(message = "Số cột ghế không được để trống")
    @Min(value = 1, message = "Số cột ghế tối thiểu là 1")
    @Max(value = 10, message = "Số cột ghế tối đa là 10")
    private Integer totalColumns;

    @Schema(description = "Mô tả / Ghi chú về phương tiện", example = "Xe chạy tuyến cao tốc")
    private String description;
}
