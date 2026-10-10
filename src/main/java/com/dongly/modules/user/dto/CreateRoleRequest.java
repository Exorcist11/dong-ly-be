package com.dongly.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Yêu cầu tạo mới vai trò người dùng (Role).
 */
public record CreateRoleRequest(
        @NotBlank(message = "Mã vai trò không được để trống")
        @Size(min = 2, max = 50, message = "Mã vai trò phải từ 2 đến 50 ký tự")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Mã vai trò chỉ chứa chữ cái, chữ số và dấu gạch dưới")
        String code,

        @NotBlank(message = "Tên vai trò không được để trống")
        @Size(max = 100, message = "Tên vai trò không được vượt quá 100 ký tự")
        String name,

        @Size(max = 255, message = "Mô tả vai trò không được vượt quá 255 ký tự")
        String description,

        Set<String> permissionCodes
) {}
