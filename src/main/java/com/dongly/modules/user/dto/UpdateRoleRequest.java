package com.dongly.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Yêu cầu cập nhật thông tin vai trò người dùng (Tên và mô tả).
 * Mã vai trò (code) không được phép sửa đổi nhằm bảo toàn tính toàn vẹn hệ thống.
 */
public record UpdateRoleRequest(
        @NotBlank(message = "Tên vai trò không được để trống")
        @Size(max = 100, message = "Tên vai trò không được vượt quá 100 ký tự")
        String name,

        @Size(max = 255, message = "Mô tả vai trò không được vượt quá 255 ký tự")
        String description
) {}
