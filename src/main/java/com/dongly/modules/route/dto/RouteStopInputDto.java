package com.dongly.modules.route.dto;

import com.dongly.modules.route.entity.RouteDirectionType;
import com.dongly.modules.route.entity.RouteStopType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteStopInputDto {

    private UUID id;

    @NotNull(message = "Điểm đón/trả vật lý không được để trống")
    private UUID stopPointId;

    @NotNull(message = "Chiều di chuyển không được để trống")
    private RouteDirectionType direction;

    @NotNull(message = "Thứ tự điểm dừng không được để trống")
    @Min(value = 1, message = "Thứ tự điểm dừng phải từ 1 trở lên")
    private Integer sequence;

    @NotNull(message = "Loại điểm dừng không được để trống")
    private RouteStopType stopType;

    @DecimalMin(value = "0.0", message = "Phụ phí không được là số âm")
    private BigDecimal extraPrice;
}
