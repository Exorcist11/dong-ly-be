package com.dongly.modules.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu giữ ghế tạm thời cho CRM Booking")
public class HoldSeatsRequest {

    @NotNull(message = "ID chuyến xe không được để trống")
    @Schema(description = "ID chuyến xe", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID tripId;

    @NotEmpty(message = "Danh sách ghế giữ không được để trống")
    @Schema(description = "Danh sách ID ghế cần giữ", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<UUID> seatIds;

    @NotBlank(message = "Số điện thoại khách hàng không được để trống")
    @Pattern(regexp = "^(0[3|5|7|8|9])[0-9]{8}$", message = "Số điện thoại không đúng định dạng Việt Nam")
    @Schema(description = "Số điện thoại khách liên hệ", example = "0912345678", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerPhone;

    @NotBlank(message = "Tên khách hàng không được để trống")
    @Schema(description = "Họ tên khách hàng", example = "Nguyễn Văn Nam", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerName;

    @Schema(description = "Ghi chú đơn đặt chỗ", example = "Khách gọi nhờ giữ phòng đôi tầng 1")
    private String note;
}
