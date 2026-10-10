package com.dongly.modules.user.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * DTO yêu cầu gán hoặc cập nhật danh sách vai trò cho người dùng.
 */
public record UpdateUserRolesRequest(
        @NotNull(message = "Danh sách mã vai trò không được null")
        Set<String> roleCodes
) {}
