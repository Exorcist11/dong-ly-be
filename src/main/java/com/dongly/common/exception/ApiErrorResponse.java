package com.dongly.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Cấu trúc JSON phản hồi lỗi chuẩn cho toàn bộ REST API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<ValidationErrorDetail> errors
) {

    public static ApiErrorResponse of(int status, String code, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, code, message, path, null);
    }

    public static ApiErrorResponse of(int status, String code, String message, String path, List<ValidationErrorDetail> errors) {
        return new ApiErrorResponse(Instant.now(), status, code, message, path, errors);
    }
}
