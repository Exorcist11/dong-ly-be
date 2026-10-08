package com.dongly.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Ngoại lệ ném ra khi không tìm thấy tài nguyên yêu cầu (HTTP 404).
 */
public class ResourceNotFoundException extends AppException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String resourceName, Object id) {
        super(ErrorCode.RESOURCE_NOT_FOUND,
                String.format("Không tìm thấy %s với định danh: %s", resourceName, id),
                HttpStatus.NOT_FOUND);
    }
}
