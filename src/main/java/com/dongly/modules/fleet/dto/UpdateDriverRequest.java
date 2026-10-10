package com.dongly.modules.fleet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDriverRequest {

    @Schema(description = "Mã tài xế (VD: TX-001)", example = "TX-003")
    @NotBlank(message = "Mã tài xế không được để trống")
    @Size(max = 30, message = "Mã tài xế không vượt quá 30 ký tự")
    private String code;

    @Schema(description = "Họ và tên tài xế", example = "Trần Văn Nam")
    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên không vượt quá 100 ký tự")
    private String fullName;

    @Schema(description = "Số điện thoại liên hệ", example = "0934567890")
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại không đúng định dạng Việt Nam")
    private String phone;

    @Schema(description = "Số giấy phép lái xe", example = "010345678912")
    @NotBlank(message = "Số GPLX không được để trống")
    @Size(max = 30, message = "Số GPLX không vượt quá 30 ký tự")
    private String licenseNumber;

    @Schema(description = "Hạng GPLX (D, E, FC...)", example = "E")
    @NotBlank(message = "Hạng GPLX không được để trống")
    @Size(max = 10, message = "Hạng GPLX không vượt quá 10 ký tự")
    private String licenseClass;

    @Schema(description = "Ngày hết hạn GPLX", example = "2029-12-31")
    private LocalDate licenseExpiryDate;

    @Schema(description = "Ngày tháng năm sinh", example = "1988-08-18")
    private LocalDate dateOfBirth;

    @Schema(description = "Ghi chú thêm về tài xế", example = "Kinh nghiệm lái xe đường dài 10 năm")
    private String note;
}
