package com.dongly.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Định nghĩa danh mục mã lỗi nghiệp vụ chuẩn trong toàn hệ thống.
 */
@Getter
public enum ErrorCode {

    // Lỗi dữ liệu & cú pháp (400 Bad Request)
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu yêu cầu không hợp lệ"),
    MALFORMED_JSON(HttpStatus.BAD_REQUEST, "Cú pháp JSON không hợp lệ hoặc thiếu dữ liệu"),
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "Tham số truyền vào không hợp lệ"),

    // Lỗi xác thực & phân quyền (401, 403)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Yêu cầu xác thực tài khoản"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Mã thông báo (Token) không hợp lệ hoặc đã hết hạn"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện hành động này"),

    // Lỗi không tìm thấy & xung đột (404, 409)
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên yêu cầu"),
    RESOURCE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Tài nguyên đã tồn tại trong hệ thống"),

    // Lỗi quy tắc nghiệp vụ & xung đột RBAC (409, 422)
    ROLE_IN_USE(HttpStatus.CONFLICT, "Vai trò đang được gán cho người dùng và không thể xóa hoặc vô hiệu hóa"),
    SYSTEM_ROLE_PROTECTED(HttpStatus.FORBIDDEN, "Không được phép chỉnh sửa hoặc xóa vai trò hệ thống"),
    BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "Vi phạm quy tắc nghiệp vụ"),

    // Lỗi hệ thống nội bộ (500)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống nội bộ. Vui lòng thử lại sau.");

    private final HttpStatus defaultStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus defaultStatus, String defaultMessage) {
        this.defaultStatus = defaultStatus;
        this.defaultMessage = defaultMessage;
    }
}
