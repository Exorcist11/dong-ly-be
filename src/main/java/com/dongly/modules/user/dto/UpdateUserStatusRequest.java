package com.dongly.modules.user.dto;

import com.dongly.modules.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "Trạng thái người dùng không được để trống")
        UserStatus status
) {}
