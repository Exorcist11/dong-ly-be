package com.dongly.security;

import com.dongly.common.exception.ApiErrorResponse;
import com.dongly.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Xử lý khi người dùng chưa xác thực truy cập vào tài nguyên yêu cầu bảo mật (HTTP 401).
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                ErrorCode.UNAUTHORIZED.name(),
                ErrorCode.UNAUTHORIZED.getDefaultMessage(),
                request.getRequestURI()
        );

        response.getOutputStream().write(objectMapper.writeValueAsBytes(errorResponse));
    }
}
