package com.dongly.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Lớp ngoại lệ nền tảng cho toàn bộ lỗi nghiệp vụ nội bộ của hệ thống.
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public AppException(ErrorCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public AppException(ErrorCode errorCode, String message) {
        this(errorCode, message, errorCode.getDefaultStatus());
    }

    public AppException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), errorCode.getDefaultStatus());
    }
}
