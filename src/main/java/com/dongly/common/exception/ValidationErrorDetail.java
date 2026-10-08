package com.dongly.common.exception;

/**
 * Chi tiết lỗi vi phạm tại từng trường dữ liệu khi xác thực (Bean Validation).
 */
public record ValidationErrorDetail(
        String field,
        Object rejectedValue,
        String message
) {}
