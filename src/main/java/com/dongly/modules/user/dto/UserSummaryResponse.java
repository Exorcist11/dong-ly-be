package com.dongly.modules.user.dto;

import java.util.Set;
import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String username,
        String email,
        String fullName,
        Set<String> roles
) {}
