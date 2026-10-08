package com.dongly.security;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Đại diện cho thông tin người dùng đã xác thực trong ngữ cảnh bảo mật hiện tại.
 */
public record CurrentUser(
        UUID id,
        String email,
        Set<String> roles
) {
    public CurrentUser {
        roles = (roles != null) ? Collections.unmodifiableSet(roles) : Collections.emptySet();
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN") || hasRole("ADMIN");
    }
}
