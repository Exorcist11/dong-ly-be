package com.dongly.modules.user.dto;

import java.util.List;

/**
 * DTO đại diện cho nhóm quyền theo phân hệ/module chức năng.
 */
public record PermissionGroupResponse(
        String module,
        List<PermissionResponse> permissions
) {}
