package com.dongly.modules.user.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * Yêu cầu gán hoặc cập nhật toàn diện danh sách quyền hạn cho vai trò.
 */
public record AssignRolePermissionsRequest(
        @NotNull(message = "Danh sách mã quyền hạn không được null")
        Set<String> permissionCodes
) {}
