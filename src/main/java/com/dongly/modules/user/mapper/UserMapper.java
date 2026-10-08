package com.dongly.modules.user.mapper;

import com.dongly.modules.auth.dto.UserProfileResponse;
import com.dongly.modules.user.dto.UserResponse;
import com.dongly.modules.user.dto.UserSummaryResponse;
import com.dongly.modules.user.entity.Permission;
import com.dongly.modules.user.entity.Role;
import com.dongly.modules.user.entity.User;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper chuyển đổi an toàn và rõ nghĩa giữa Entity và DTOs.
 */
public final class UserMapper {

    private UserMapper() {
        // Lớp tiện ích không cho phép khởi tạo
    }

    public static UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        Set<String> roleCodes = user.getRoles() != null
                ? user.getRoles().stream()
                        .filter(Objects::nonNull)
                        .map(Role::getCode)
                        .collect(Collectors.toSet())
                : Collections.emptySet();

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getStatus(),
                roleCodes,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public static UserSummaryResponse toSummary(User user) {
        if (user == null) {
            return null;
        }

        Set<String> roleCodes = user.getRoles() != null
                ? user.getRoles().stream()
                        .filter(Objects::nonNull)
                        .map(Role::getCode)
                        .collect(Collectors.toSet())
                : Collections.emptySet();

        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                roleCodes
        );
    }

    public static UserProfileResponse toProfile(User user) {
        if (user == null) {
            return null;
        }

        Set<String> roleCodes = user.getRoles() != null
                ? user.getRoles().stream()
                        .filter(Objects::nonNull)
                        .map(Role::getCode)
                        .collect(Collectors.toSet())
                : Collections.emptySet();

        Set<String> permissionCodes = user.getRoles() != null
                ? user.getRoles().stream()
                        .filter(Objects::nonNull)
                        .map(Role::getPermissions)
                        .filter(Objects::nonNull)
                        .flatMap(Set::stream)
                        .filter(Objects::nonNull)
                        .map(Permission::getCode)
                        .collect(Collectors.toSet())
                : Collections.emptySet();

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getStatus(),
                roleCodes,
                permissionCodes,
                user.getCreatedAt()
        );
    }
}
