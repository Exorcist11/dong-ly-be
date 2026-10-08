package com.dongly.security;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Đại diện cho thông tin người dùng đã xác thực trong ngữ cảnh bảo mật hiện tại.
 */
public record CurrentUser(
        UUID id,
        String username,
        String email,
        Set<String> roles,
        Set<String> permissions
) {
    public CurrentUser {
        roles = (roles != null) ? Collections.unmodifiableSet(roles) : Collections.emptySet();
        permissions = (permissions != null) ? Collections.unmodifiableSet(permissions) : Collections.emptySet();
    }

    public CurrentUser(UUID id, String email, Set<String> roles) {
        this(id, email, email, roles, Collections.emptySet());
    }

    public boolean hasRole(String role) {
        return roles.contains(role) || roles.contains("ROLE_" + role);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN") || hasRole("ADMIN");
    }
}
