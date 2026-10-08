package com.dongly.modules.user.dto;

import com.dongly.modules.user.entity.UserStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String fullName,
        String phone,
        UserStatus status,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt
) {}
