package com.dongly.modules.user.dto;

import java.util.List;

/**
 * DTO phản hồi danh mục quyền hạn toàn hệ thống kèm cấu trúc nhóm theo module.
 */
public record PermissionCatalogResponse(
        int totalPermissions,
        List<PermissionGroupResponse> modules,
        List<PermissionResponse> permissions
) {}
