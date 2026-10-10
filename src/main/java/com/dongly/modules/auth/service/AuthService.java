package com.dongly.modules.auth.service;

import com.dongly.common.exception.AppException;
import com.dongly.common.exception.ErrorCode;
import com.dongly.common.exception.ResourceNotFoundException;
import com.dongly.modules.auth.dto.LoginRequest;
import com.dongly.modules.auth.dto.LogoutRequest;
import com.dongly.modules.auth.dto.RefreshTokenRequest;
import com.dongly.modules.auth.dto.TokenResponse;
import com.dongly.modules.auth.dto.UserProfileResponse;
import com.dongly.modules.auth.entity.RefreshToken;
import com.dongly.modules.auth.repository.RefreshTokenRepository;
import com.dongly.modules.user.entity.Permission;
import com.dongly.modules.user.entity.Role;
import com.dongly.modules.user.entity.User;
import com.dongly.modules.user.entity.UserStatus;
import com.dongly.modules.user.mapper.UserMapper;
import com.dongly.modules.user.repository.UserRepository;
import com.dongly.security.CurrentUser;
import com.dongly.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Xử lý toàn bộ logic nghiệp vụ xác thực (Login, Refresh, Logout, Profile).
 */
@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Xác thực thông tin đăng nhập và cấp phát bộ mã thông báo (Access & Refresh Token).
     * Theo yêu cầu nghiệp vụ: chỉ trả về token, không kèm thông tin người dùng.
     */
    @Transactional
    public TokenResponse login(LoginRequest request, String clientIp) {
        String identifier = request.username().trim();

        User user = userRepository.findByUsernameOrEmailIgnoreCase(identifier)
                .orElseThrow(() -> new AppException(
                        ErrorCode.UNAUTHORIZED,
                        "Tên đăng nhập hoặc mật khẩu không chính xác"
                ));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Đăng nhập thất bại: sai mật khẩu cho định danh '{}'", identifier);
            throw new AppException(
                    ErrorCode.UNAUTHORIZED,
                    "Tên đăng nhập hoặc mật khẩu không chính xác"
            );
        }

        if (UserStatus.LOCKED.equals(user.getStatus())) {
            log.warn("Đăng nhập thất bại: tài khoản '{}' đang bị khóa", user.getUsername());
            throw new AppException(
                    ErrorCode.UNAUTHORIZED,
                    "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên."
            );
        }

        if (UserStatus.INACTIVE.equals(user.getStatus())) {
            log.warn("Đăng nhập thất bại: tài khoản '{}' chưa kích hoạt/ngừng hoạt động", user.getUsername());
            throw new AppException(
                    ErrorCode.UNAUTHORIZED,
                    "Tài khoản của bạn chưa được kích hoạt hoặc đã ngừng hoạt động."
            );
        }

        Set<String> roleCodes = extractRoleCodes(user);
        Set<String> permissionCodes = extractPermissionCodes(user);

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roleCodes,
                permissionCodes
        );

        String rawRefreshToken = generateSecureRandomToken();
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds()))
                .createdByIp(clientIp)
                .build();

        refreshTokenRepository.save(refreshToken);

        return TokenResponse.of(
                accessToken,
                rawRefreshToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    /**
     * Làm mới Access Token thông qua Refresh Token hợp lệ (Áp dụng xoay vòng Token Rotation).
     */
    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request, String clientIp) {
        String rawToken = request.refreshToken().trim();
        String tokenHash = hashToken(rawToken);

        RefreshToken currentRefreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AppException(
                        ErrorCode.INVALID_TOKEN,
                        "Mã làm mới (Refresh Token) không hợp lệ hoặc không tồn tại"
                ));

        // Phát hiện tái sử dụng Refresh Token đã thu hồi (Dấu hiệu Replay Attack / Token Thefts)
        if (currentRefreshToken.isRevoked()) {
            log.error("CẢNH BÁO AN NINH: Phát hiện cố gắng tái sử dụng Refresh Token đã bị thu hồi của user ID: {}. Tiến hành thu hồi toàn bộ phiên của người dùng.",
                    currentRefreshToken.getUser().getId());
            refreshTokenRepository.revokeAllActiveTokensByUserId(currentRefreshToken.getUser().getId());
            throw new AppException(
                    ErrorCode.INVALID_TOKEN,
                    "Mã làm mới đã bị thu hồi hoặc đã từng được sử dụng. Vì lý do bảo mật, vui lòng đăng nhập lại."
            );
        }

        if (currentRefreshToken.isExpired()) {
            throw new AppException(
                    ErrorCode.INVALID_TOKEN,
                    "Mã làm mới (Refresh Token) đã hết hạn. Vui lòng đăng nhập lại."
            );
        }

        User user = currentRefreshToken.getUser();
        if (!user.isActive()) {
            throw new AppException(
                    ErrorCode.UNAUTHORIZED,
                    "Tài khoản người dùng không ở trạng thái hoạt động."
            );
        }

        // 1. Thu hồi token hiện tại (Token Rotation)
        currentRefreshToken.revoke();
        refreshTokenRepository.save(currentRefreshToken);

        // 2. Tạo Refresh Token mới thay thế
        String newRawRefreshToken = generateSecureRandomToken();
        String newTokenHash = hashToken(newRawRefreshToken);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(newTokenHash)
                .expiresAt(Instant.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds()))
                .createdByIp(clientIp)
                .build();

        refreshTokenRepository.save(newRefreshToken);

        Set<String> roleCodes = extractRoleCodes(user);
        Set<String> permissionCodes = extractPermissionCodes(user);

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roleCodes,
                permissionCodes
        );

        return TokenResponse.of(
                newAccessToken,
                newRawRefreshToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    /**
     * Đăng xuất và vô hiệu hóa phiên làm việc của Refresh Token tương ứng của chính người dùng hiện tại.
     */
    @Transactional
    public void logout(LogoutRequest request, CurrentUser currentUser) {
        String rawToken = request.refreshToken().trim();
        String tokenHash = hashToken(rawToken);

        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    if (!token.getUser().getId().equals(currentUser.id())) {
                        throw new AppException(ErrorCode.ACCESS_DENIED, "Không có quyền thu hồi phiên làm việc của người dùng khác");
                    }
                    token.revoke();
                    refreshTokenRepository.save(token);
                });
    }

    /**
     * Lấy thông tin hồ sơ chi tiết của người dùng đã xác thực (kèm roles và permissions).
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(CurrentUser currentUser) {
        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", currentUser.id()));

        return UserMapper.toProfile(user);
    }

    private Set<String> extractRoleCodes(User user) {
        if (user.getRoles() == null) {
            return Collections.emptySet();
        }
        return user.getRoles().stream()
                .filter(Objects::nonNull)
                .filter(Role::isActive)
                .map(Role::getCode)
                .collect(Collectors.toSet());
    }

    private Set<String> extractPermissionCodes(User user) {
        if (user.getRoles() == null) {
            return Collections.emptySet();
        }
        return user.getRoles().stream()
                .filter(Objects::nonNull)
                .filter(Role::isActive)
                .map(Role::getPermissions)
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .filter(Objects::nonNull)
                .map(Permission::getCode)
                .collect(Collectors.toSet());
    }

    private String generateSecureRandomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Lỗi thuật toán băm SHA-256", e);
        }
    }
}
