package com.dongly.modules.booking.dto;

import com.dongly.modules.route.entity.RouteStop;
import com.dongly.modules.route.entity.RouteStopType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin điểm đón/trả khả dụng trên chuyến xe")
public class CrmTripStopDto {

    @Schema(description = "ID điểm dừng trên tuyến")
    private UUID routeStopId;

    @Schema(description = "ID điểm dừng vật lý")
    private UUID stopPointId;

    @Schema(description = "Tên điểm đón/trả", example = "Big C Thăng Long")
    private String name;

    @Schema(description = "Địa chỉ chi tiết", example = "222 Trần Duy Hưng, Cầu Giấy, Hà Nội")
    private String address;

    @Schema(description = "Loại điểm dừng (PICKUP, DROPOFF, BOTH)")
    private RouteStopType stopType;

    @Schema(description = "Thứ tự trên hành trình", example = "1")
    private int sequence;

    @Schema(description = "Phụ phí đón/trả tại điểm này (VNĐ)", example = "20000.00")
    private BigDecimal extraPrice;

    public static CrmTripStopDto fromEntity(RouteStop rs) {
        if (rs == null) return null;

        CrmTripStopDtoBuilder builder = CrmTripStopDto.builder()
                .routeStopId(rs.getId())
                .stopType(rs.getStopType())
                .sequence(rs.getSequence())
                .extraPrice(rs.getExtraPrice());

        if (rs.getStopPoint() != null) {
            builder.stopPointId(rs.getStopPoint().getId())
                    .name(rs.getStopPoint().getName())
                    .address(rs.getStopPoint().getAddress());
        }

        return builder.build();
    }
}
