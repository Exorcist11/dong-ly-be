package com.dongly.security;

import com.dongly.common.exception.ApiErrorResponse;
import com.dongly.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Xử lý khi người dùng đã xác thực nhưng không có quyền hạn truy cập tài nguyên (HTTP 403).
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JwtAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.FORBIDDEN.value());

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                ErrorCode.ACCESS_DENIED.name(),
                ErrorCode.ACCESS_DENIED.getDefaultMessage(),
                request.getRequestURI()
        );

        response.getOutputStream().write(objectMapper.writeValueAsBytes(errorResponse));
    }
}
