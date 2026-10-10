package com.dongly.modules.user.dto;

/**
 * DTO đại diện cho thông tin quyền hạn hạt nhân trong danh mục RBAC.
 */
public record PermissionResponse(
        String code,
        String name,
        String description,
        String module,
        String action
) {}
