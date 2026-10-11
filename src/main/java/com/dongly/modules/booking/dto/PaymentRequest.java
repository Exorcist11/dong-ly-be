package com.dongly.modules.booking.dto;

import com.dongly.modules.booking.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu thanh toán khi xác nhận đơn đặt vé")
public class PaymentRequest {

    @NotNull(message = "Phương thức thanh toán không được để trống")
    @Schema(description = "Phương thức thanh toán (CASH, BANK_TRANSFER, VIETQR)", example = "CASH", requiredMode = Schema.RequiredMode.REQUIRED)
    private PaymentMethod method;

    @NotNull(message = "Số tiền thanh toán không được để trống")
    @DecimalMin(value = "0.00", message = "Số tiền thanh toán phải lớn hơn hoặc bằng 0")
    @Schema(description = "Số tiền thanh toán (VNĐ)", example = "600000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Schema(description = "Mã giao dịch ngân hàng / POS (nếu có)")
    private String transactionCode;

    @Schema(description = "Ghi chú thanh toán")
    private String note;
}
