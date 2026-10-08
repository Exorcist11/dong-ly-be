package com.dongly.common.api;

import java.time.Instant;

/**
 * Cấu trúc đóng gói dữ liệu phản hồi API chuẩn cho toàn hệ thống.
 *
 * @param <T> kiểu dữ liệu trả về
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Thao tác thành công", data, Instant.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(true, "Thao tác thành công", null, Instant.now());
    }

    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(true, message, null, Instant.now());
    }
}
