package com.dongly.modules.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu xác nhận đơn đặt vé và xuất vé")
public class ConfirmBookingRequest {

    @NotBlank(message = "Tên khách hàng không được để trống")
    @Schema(description = "Họ tên khách hàng", example = "Nguyễn Văn Nam", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerName;

    @NotBlank(message = "Số điện thoại khách hàng không được để trống")
    @Pattern(regexp = "^(0[3|5|7|8|9])[0-9]{8}$", message = "Số điện thoại không đúng định dạng Việt Nam")
    @Schema(description = "Số điện thoại khách hàng", example = "0912345678", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerPhone;

    @Schema(description = "Email khách hàng (tùy chọn)", example = "nam@gmail.com")
    private String customerEmail;

    @NotEmpty(message = "Danh sách hành khách không được để trống")
    @Valid
    @Schema(description = "Danh sách chi tiết hành khách theo từng ghế", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<PassengerItemRequest> passengers;

    @NotNull(message = "Thông tin thanh toán không được để trống")
    @Valid
    @Schema(description = "Thông tin thanh toán bắt buộc", requiredMode = Schema.RequiredMode.REQUIRED)
    private PaymentRequest payment;

    @Schema(description = "Ghi chú đơn vé")
    private String note;
}
