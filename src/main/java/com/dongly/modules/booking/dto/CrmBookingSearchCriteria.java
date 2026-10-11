package com.dongly.modules.booking.dto;

import com.dongly.modules.booking.entity.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Tiêu chí tìm kiếm và phân trang đơn đặt vé CRM")
public class CrmBookingSearchCriteria {

    @Schema(description = "Từ khóa tìm kiếm (Mã booking, họ tên khách, số điện thoại, biển số xe)")
    private String keyword;

    @Schema(description = "Trạng thái đơn đặt vé (HELD, CONFIRMED, CANCELLED, EXPIRED, COMPLETED)")
    private BookingStatus status;

    @Schema(description = "ID chuyến xe")
    private UUID tripId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Ngày khởi hành chuyến xe từ ngày (YYYY-MM-DD)")
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Ngày khởi hành chuyến xe đến ngày (YYYY-MM-DD)")
    private LocalDate toDate;

    @Builder.Default
    @Schema(description = "Số trang (bắt đầu từ 0)", example = "0")
    private Integer page = 0;

    @Builder.Default
    @Schema(description = "Kích thước trang (mặc định 20)", example = "20")
    private Integer size = 20;

    @Builder.Default
    @Schema(description = "Sắp xếp theo trường (mặc định createdAt,desc)", example = "createdAt,desc")
    private String sort = "createdAt,desc";
}
