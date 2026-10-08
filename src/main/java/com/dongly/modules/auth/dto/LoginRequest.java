package com.dongly.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Tên đăng nhập hoặc email không được để trống")
        String username,

        @NotBlank(message = "Mật khẩu không được để trống")
        String password
) {}
