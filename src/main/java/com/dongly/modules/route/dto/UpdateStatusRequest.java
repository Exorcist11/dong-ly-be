package com.dongly.modules.route.dto;

import com.dongly.modules.route.entity.CommonStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private CommonStatus status;
}
