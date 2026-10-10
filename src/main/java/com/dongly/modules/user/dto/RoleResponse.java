package com.dongly.modules.user.dto;

import com.dongly.modules.user.entity.RoleStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO phản hồi thông tin tổng quát vai trò trong danh sách.
 */
public record RoleResponse(
        UUID id,
        String code,
        String name,
        String description,
        RoleStatus status,
        boolean isSystem,
        int permissionCount,
        Instant createdAt,
        Instant updatedAt
) {}
