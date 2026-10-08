package com.dongly.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Ngoại lệ ném ra khi vi phạm quy tắc nghiệp vụ hệ thống (HTTP 422 Unprocessable Entity).
 */
public class BusinessRuleException extends AppException {

    public BusinessRuleException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public BusinessRuleException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
