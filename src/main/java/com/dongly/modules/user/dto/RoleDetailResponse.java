package com.dongly.modules.user.dto;

import com.dongly.modules.user.entity.RoleStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * DTO phản hồi thông tin chi tiết vai trò kèm toàn bộ quyền hạn được gán.
 */
public record RoleDetailResponse(
        UUID id,
        String code,
        String name,
        String description,
        RoleStatus status,
        boolean isSystem,
        Set<PermissionResponse> permissions,
        Instant createdAt,
        Instant updatedAt
) {}
