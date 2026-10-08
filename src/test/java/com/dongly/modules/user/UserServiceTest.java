package com.dongly.modules.user;

import com.dongly.common.exception.AppException;
import com.dongly.common.exception.ErrorCode;
import com.dongly.modules.auth.repository.RefreshTokenRepository;
import com.dongly.modules.user.dto.CreateUserRequest;
import com.dongly.modules.user.dto.UpdateUserStatusRequest;
import com.dongly.modules.user.dto.UserResponse;
import com.dongly.modules.user.entity.Role;
import com.dongly.modules.user.entity.User;
import com.dongly.modules.user.entity.UserStatus;
import com.dongly.modules.user.repository.RoleRepository;
import com.dongly.modules.user.repository.UserRepository;
import com.dongly.modules.user.service.UserService;
import com.dongly.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private CurrentUser adminUser;
    private Role staffRole;

    @BeforeEach
    void setUp() {
        adminUser = new CurrentUser(
                UUID.randomUUID(),
                "admin",
                "admin@dongly.vn",
                Set.of("ADMIN"),
                Set.of("USER_READ", "USER_CREATE", "USER_UPDATE")
        );

        staffRole = Role.builder()
                .id(UUID.randomUUID())
                .code("STAFF")
                .name("Nhân viên")
                .build();
    }

    @Test
    @DisplayName("Tạo người dùng thành công: mã hóa mật khẩu và gán vai trò")
    void createUser_success_hashesPasswordAndAssignsRoles() {
        CreateUserRequest request = new CreateUserRequest(
                "staff1",
                "staff1@dongly.vn",
                "PlainPassword123",
                "Nhân viên 1",
                "0912345678",
                Set.of("STAFF")
        );

        when(userRepository.existsByUsername("staff1")).thenReturn(false);
        when(userRepository.existsByEmail("staff1@dongly.vn")).thenReturn(false);
        when(passwordEncoder.encode("PlainPassword123")).thenReturn("$2a$12$hashedPassword");
        when(roleRepository.findAllByCodeIn(Set.of("STAFF"))).thenReturn(List.of(staffRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserResponse response = userService.createUser(request, adminUser);

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("staff1");
        assertThat(response.email()).isEqualTo("staff1@dongly.vn");
        assertThat(response.roles()).contains("STAFF");
        verify(passwordEncoder).encode("PlainPassword123");
    }

    @Test
    @DisplayName("Tạo người dùng thất bại khi trùng tên đăng nhập ném lỗi RESOURCE_ALREADY_EXISTS")
    void createUser_duplicateUsername_throwsConflictException() {
        CreateUserRequest request = new CreateUserRequest(
                "admin",
                "new@dongly.vn",
                "Password123",
                "Admin Trùng",
                null,
                null
        );

        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request, adminUser))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.RESOURCE_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("Lấy chi tiết người dùng thất bại nếu không có quyền và không phải chính chủ (Chống IDOR)")
    void getUserById_asOtherUserWithoutPermission_throwsAccessDenied() {
        UUID targetId = UUID.randomUUID();
        CurrentUser normalUser = new CurrentUser(
                UUID.randomUUID(), // Khác targetId
                "user1",
                "user1@dongly.vn",
                Set.of("CUSTOMER"),
                Set.of() // Không có USER_READ
        );

        assertThatThrownBy(() -> userService.getUserById(targetId, normalUser))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("Cập nhật trạng thái thành INACTIVE thành công và tự động thu hồi refresh token của user")
    void updateStatus_toInactive_revokesTokens() {
        UUID targetId = UUID.randomUUID();
        User user = User.builder()
                .id(targetId)
                .username("user_to_block")
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(targetId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.INACTIVE);
        UserResponse response = userService.updateUserStatus(targetId, request, adminUser);

        assertThat(response.status()).isEqualTo(UserStatus.INACTIVE);
        verify(refreshTokenRepository).revokeAllActiveTokensByUserId(targetId);
    }

    @Test
    @DisplayName("Quản trị viên không thể tự khóa tài khoản của chính mình")
    void updateStatus_adminSelfLock_throwsBusinessRuleException() {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.LOCKED);
        User adminEntity = User.builder().id(adminUser.id()).username("admin").build();

        when(userRepository.findById(adminUser.id())).thenReturn(Optional.of(adminEntity));

        assertThatThrownBy(() -> userService.updateUserStatus(adminUser.id(), request, adminUser))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
    }
}
