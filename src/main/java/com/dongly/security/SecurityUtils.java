package com.dongly.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Tiện ích hỗ trợ truy xuất nhanh thông tin người dùng hiện tại từ SecurityContextHolder.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Lớp tiện ích không cho phép khởi tạo đối tượng
    }

    /**
     * Lấy đối tượng CurrentUser nếu request đã được xác thực thành công.
     */
    public static Optional<CurrentUser> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CurrentUser currentUser) {
            return Optional.of(currentUser);
        }
        return Optional.empty();
    }

    /**
     * Lấy ID của người dùng hiện tại nếu có.
     */
    public static Optional<UUID> getCurrentUserId() {
        return getCurrentUser().map(CurrentUser::id);
    }
}
