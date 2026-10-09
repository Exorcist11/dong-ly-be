package com.dongly.modules.auth.controller;

import com.dongly.common.api.ApiResponse;
import com.dongly.common.exception.AppException;
import com.dongly.common.exception.ErrorCode;
import com.dongly.modules.auth.dto.LoginRequest;
import com.dongly.modules.auth.dto.LogoutRequest;
import com.dongly.modules.auth.dto.RefreshTokenRequest;
import com.dongly.modules.auth.dto.TokenResponse;
import com.dongly.modules.auth.dto.UserProfileResponse;
import com.dongly.modules.auth.service.AuthService;
import com.dongly.security.CurrentUser;
import com.dongly.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller tiếp nhận các yêu cầu xác thực người dùng (Login, Refresh, Logout, Profile).
 */
@Tag(name = "Authentication", description = "Các endpoint xác thực người dùng và quản lý phiên làm việc")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Đăng nhập hệ thống (chỉ trả về Token tương ứng)")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        TokenResponse tokenResponse = authService.login(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", tokenResponse));
    }

    @Operation(summary = "Làm mới Access Token thông qua Refresh Token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        TokenResponse tokenResponse = authService.refresh(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success("Làm mới mã thông báo thành công", tokenResponse));
    }

    @Operation(summary = "Đăng xuất tài khoản và thu hồi Refresh Token")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Yêu cầu xác thực tài khoản"));

        authService.logout(request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Đăng xuất thành công"));
    }

    @Operation(summary = "Lấy thông tin hồ sơ người dùng hiện tại kèm vai trò và quyền hạn")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser() {
        CurrentUser currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Yêu cầu xác thực tài khoản"));

        UserProfileResponse profile = authService.getCurrentUserProfile(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", profile));
    }

    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
