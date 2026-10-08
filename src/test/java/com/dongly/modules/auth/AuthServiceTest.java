package com.dongly.modules.auth;

import com.dongly.common.exception.AppException;
import com.dongly.common.exception.ErrorCode;
import com.dongly.modules.auth.dto.LoginRequest;
import com.dongly.modules.auth.dto.LogoutRequest;
import com.dongly.modules.auth.dto.RefreshTokenRequest;
import com.dongly.modules.auth.dto.TokenResponse;
import com.dongly.modules.auth.dto.UserProfileResponse;
import com.dongly.modules.auth.entity.RefreshToken;
import com.dongly.modules.auth.repository.RefreshTokenRepository;
import com.dongly.modules.auth.service.AuthService;
import com.dongly.modules.user.entity.Permission;
import com.dongly.modules.user.entity.Role;
import com.dongly.modules.user.entity.User;
import com.dongly.modules.user.entity.UserStatus;
import com.dongly.modules.user.repository.UserRepository;
import com.dongly.security.CurrentUser;
import com.dongly.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private Role adminRole;
    private Permission readPerm;

    @BeforeEach
    void setUp() {
        readPerm = Permission.builder()
                .id(UUID.randomUUID())
                .code("USER_READ")
                .name("Xem người dùng")
                .build();

        adminRole = Role.builder()
                .id(UUID.randomUUID())
                .code("ADMIN")
                .name("Quản trị viên")
                .permissions(Set.of(readPerm))
                .build();

        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .email("admin@dongly.vn")
                .passwordHash("$2a$12$hashedPassword...")
                .fullName("Quản trị viên Đông Lý")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build();
    }

    @Test
    @DisplayName("Đăng nhập thành công chỉ trả về TokenResponse (không chứa thông tin user)")
    void login_success_returnsOnlyTokens() {
        LoginRequest request = new LoginRequest("admin", "Password@123");

        when(userRepository.findByUsernameOrEmailIgnoreCase("admin")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password@123", sampleUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(eq(sampleUser.getId()), eq("admin"), eq("admin@dongly.vn"), anyCollection(), anyCollection()))
                .thenReturn("mock-access-token");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(1800L);
        when(jwtTokenProvider.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        TokenResponse response = authService.login(request, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("mock-access-token");
        assertThat(response.refreshToken()).isNotNull().isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(1800L);

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Đăng nhập thất bại do sai mật khẩu ném lỗi UNAUTHORIZED chung")
    void login_wrongPassword_throwsUnauthorizedException() {
        LoginRequest request = new LoginRequest("admin", "WrongPassword");

        when(userRepository.findByUsernameOrEmailIgnoreCase("admin")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPassword", sampleUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
                .isInstanceOf(AppException.class)
                .hasMessage("Tên đăng nhập hoặc mật khẩu không chính xác")
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi tài khoản bị khóa ném lỗi kèm cảnh báo")
    void login_lockedUser_throwsUnauthorizedException() {
        sampleUser.setStatus(UserStatus.LOCKED);
        LoginRequest request = new LoginRequest("admin", "Password@123");

        when(userRepository.findByUsernameOrEmailIgnoreCase("admin")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password@123", sampleUser.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
                .isInstanceOf(AppException.class)
                .hasMessage("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.")
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Làm mới token thành công thực hiện xoay vòng token (Token Rotation)")
    void refresh_success_rotatesTokens() {
        String rawToken = "sample-raw-refresh-token";
        String tokenHash = AuthService.hashToken(rawToken);

        RefreshToken oldToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        RefreshTokenRequest request = new RefreshTokenRequest(rawToken);

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(oldToken));
        when(jwtTokenProvider.getRefreshTokenExpirationSeconds()).thenReturn(604800L);
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(1800L);
        when(jwtTokenProvider.generateAccessToken(eq(sampleUser.getId()), eq("admin"), eq("admin@dongly.vn"), anyCollection(), anyCollection()))
                .thenReturn("new-mock-access-token");

        TokenResponse response = authService.refresh(request, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("new-mock-access-token");
        assertThat(oldToken.isRevoked()).isTrue();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        // Lưu 2 lần: 1 lần thu hồi token cũ, 1 lần tạo token mới
        verify(refreshTokenRepository, times(2)).save(captor.capture());
    }

    @Test
    @DisplayName("Phát hiện tấn công Replay khi refresh token đã thu hồi và tự động hủy mọi phiên của user")
    void refresh_revokedToken_triggersReplayAttackProtection() {
        String rawToken = "revoked-token";
        String tokenHash = AuthService.hashToken(rawToken);

        RefreshToken revokedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .revokedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        RefreshTokenRequest request = new RefreshTokenRequest(rawToken);

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> authService.refresh(request, "127.0.0.1"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_TOKEN);

        verify(refreshTokenRepository).revokeAllActiveTokensByUserId(sampleUser.getId());
    }

    @Test
    @DisplayName("Đăng xuất thành công đánh dấu thu hồi Refresh Token tương ứng")
    void logout_revokesToken() {
        String rawToken = "active-token";
        String tokenHash = AuthService.hashToken(rawToken);

        RefreshToken activeToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(activeToken));

        authService.logout(new LogoutRequest(rawToken));

        assertThat(activeToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(activeToken);
    }

    @Test
    @DisplayName("Lấy thông tin profile người dùng hiện tại trả về đầy đủ roles và permissions")
    void getCurrentUserProfile_success_returnsProfileWithRolesAndPermissions() {
        CurrentUser currentUser = new CurrentUser(
                sampleUser.getId(),
                sampleUser.getUsername(),
                sampleUser.getEmail(),
                Set.of("ADMIN"),
                Set.of("USER_READ")
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UserProfileResponse profile = authService.getCurrentUserProfile(currentUser);

        assertThat(profile).isNotNull();
        assertThat(profile.id()).isEqualTo(sampleUser.getId());
        assertThat(profile.username()).isEqualTo("admin");
        assertThat(profile.email()).isEqualTo("admin@dongly.vn");
        assertThat(profile.roles()).contains("ADMIN");
        assertThat(profile.permissions()).contains("USER_READ");
    }
}
