package com.dongly.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Xử lý ngoại lệ tập trung cho toàn bộ ứng dụng REST API.
 * Đảm bảo mọi phản hồi lỗi đều tuân thủ cấu trúc JSON thống nhất và an toàn thông tin.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bắt lỗi vi phạm kiểm tra hợp lệ DTO (@Valid RequestBody).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ValidationErrorDetail> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new ValidationErrorDetail(
                        fieldError.getField(),
                        fieldError.getRejectedValue(),
                        fieldError.getDefaultMessage()
                ))
                .toList();

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.VALIDATION_ERROR.name(),
                ErrorCode.VALIDATION_ERROR.getDefaultMessage(),
                request.getRequestURI(),
                validationErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Bắt lỗi vi phạm ràng buộc trên tham số (@Validated trên Controller class).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        List<ValidationErrorDetail> validationErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> new ValidationErrorDetail(
                        violation.getPropertyPath().toString(),
                        violation.getInvalidValue(),
                        violation.getMessage()
                ))
                .toList();

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.VALIDATION_ERROR.name(),
                ErrorCode.VALIDATION_ERROR.getDefaultMessage(),
                request.getRequestURI(),
                validationErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Bắt lỗi JSON sai định dạng cú pháp hoặc không thể giải mã.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        log.warn("Lỗi đọc payload HTTP tại {}: {}", request.getRequestURI(), ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.MALFORMED_JSON.name(),
                ErrorCode.MALFORMED_JSON.getDefaultMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Bắt lỗi sai kiểu dữ liệu tham số trên URL (ví dụ: chuỗi không phải UUID hợp lệ).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String message = String.format("Tham số '%s' có giá trị không hợp lệ", ex.getName());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.INVALID_ARGUMENT.name(),
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Bắt lỗi không tìm thấy tài nguyên endpoint (Spring 6+ NoResourceFoundException).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                ErrorCode.RESOURCE_NOT_FOUND.name(),
                ErrorCode.RESOURCE_NOT_FOUND.getDefaultMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Bắt toàn bộ ngoại lệ nghiệp vụ nội bộ của hệ thống (kế thừa từ AppException).
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiErrorResponse> handleAppException(
            AppException ex,
            HttpServletRequest request
    ) {
        log.warn("Ngoại lệ nghiệp vụ [{}] tại {}: {}",
                ex.getErrorCode(), request.getRequestURI(), ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
                ex.getHttpStatus().value(),
                ex.getErrorCode().name(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    /**
     * Bắt lỗi Spring Security: Người dùng không có quyền truy cập tài nguyên (403 Forbidden).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        log.warn("Từ chối truy cập tại {}: {}", request.getRequestURI(), ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                ErrorCode.ACCESS_DENIED.name(),
                ErrorCode.ACCESS_DENIED.getDefaultMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Bắt lỗi Spring Security: Người dùng chưa được xác thực hoặc token không hợp lệ (401 Unauthorized).
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        log.warn("Lỗi xác thực tại {}: {}", request.getRequestURI(), ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                ErrorCode.UNAUTHORIZED.name(),
                ErrorCode.UNAUTHORIZED.getDefaultMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Bắt mọi ngoại lệ chưa lường trước (Fallback tổng quát 500 Internal Server Error).
     * Ghi log đầy đủ stack trace nội bộ nhưng tuyệt đối không gửi chi tiết cho client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnhandledException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Lỗi hệ thống nội bộ chưa xử lý tại {}:", request.getRequestURI(), ex);

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ErrorCode.INTERNAL_SERVER_ERROR.name(),
                ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
