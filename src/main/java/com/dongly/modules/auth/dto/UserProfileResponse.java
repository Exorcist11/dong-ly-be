package com.dongly.modules.auth.dto;

import com.dongly.modules.user.entity.UserStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Phản hồi hồ sơ chi tiết của người dùng hiện tại kèm roles và permissions.
 */
public record UserProfileResponse(
        UUID id,
        String username,
        String email,
        String fullName,
        String phone,
        UserStatus status,
        Set<String> roles,
        Set<String> permissions,
        Instant createdAt
) {}
