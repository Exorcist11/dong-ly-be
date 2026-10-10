package com.dongly.modules.user.dto;

import com.dongly.modules.user.entity.RoleStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Yêu cầu cập nhật trạng thái hoạt động của vai trò (ACTIVE / INACTIVE).
 */
public record UpdateRoleStatusRequest(
        @NotNull(message = "Trạng thái vai trò không được để trống")
        RoleStatus status
) {}
