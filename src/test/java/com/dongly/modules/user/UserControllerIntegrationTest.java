package com.dongly.modules.user;

import com.dongly.modules.auth.entity.RefreshToken;
import com.dongly.modules.auth.repository.RefreshTokenRepository;
import com.dongly.modules.user.entity.Permission;
import com.dongly.modules.user.entity.Role;
import com.dongly.modules.user.entity.RoleStatus;
import com.dongly.modules.user.entity.User;
import com.dongly.modules.user.entity.UserStatus;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.dongly.modules.user.repository.PermissionRepository;
import com.dongly.modules.user.repository.RoleRepository;
import com.dongly.modules.user.repository.UserRepository;
import com.dongly.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User staffUser;
    private User customerUser;
    private Role adminRole;
    private Role staffRole;
    private Role inactiveRole;
    private Role privilegedCustomRole;
    private String adminToken;
    private String operatorToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        Permission pRead = permissionRepository.save(Permission.builder().code("USER_READ").name("Xem").build());
        Permission pCreate = permissionRepository.save(Permission.builder().code("USER_CREATE").name("Tạo").build());
        Permission pUpdate = permissionRepository.save(Permission.builder().code("USER_UPDATE").name("Sửa").build());
        Permission pRoleAssign = permissionRepository.save(Permission.builder().code("ROLE_ASSIGN").name("Gán role").build());
        Permission pSecretPerm = permissionRepository.save(Permission.builder().code("SECRET_ACTION").name("Hành động tối mật").build());

        adminRole = roleRepository.save(Role.builder()
                .code("ADMIN")
                .name("Quản trị viên")
                .status(RoleStatus.ACTIVE)
                .isSystem(true)
                .permissions(new HashSet<>(Set.of(pRead, pCreate, pUpdate, pRoleAssign, pSecretPerm)))
                .build());

        staffRole = roleRepository.save(Role.builder()
                .code("STAFF")
                .name("Nhân viên")
                .status(RoleStatus.ACTIVE)
                .isSystem(false)
                .permissions(new HashSet<>(Set.of(pRead)))
                .build());

        inactiveRole = roleRepository.save(Role.builder()
                .code("INACTIVE_ROLE")
                .name("Vai trò tạm khóa")
                .status(RoleStatus.INACTIVE)
                .isSystem(false)
                .permissions(new HashSet<>())
                .build());

        privilegedCustomRole = roleRepository.save(Role.builder()
                .code("SECRET_ROLE")
                .name("Vai trò bí mật")
                .status(RoleStatus.ACTIVE)
                .isSystem(false)
                .permissions(new HashSet<>(Set.of(pSecretPerm)))
                .build());

        adminUser = userRepository.save(User.builder()
                .username("system_admin")
                .email("sysadmin@dongly.vn")
                .passwordHash(passwordEncoder.encode("AdminPass123"))
                .fullName("Quản trị hệ thống")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        staffUser = userRepository.save(User.builder()
                .username("staff_member")
                .email("staff@dongly.vn")
                .passwordHash(passwordEncoder.encode("StaffPass123"))
                .fullName("Nhân viên Văn phòng")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build());

        customerUser = userRepository.save(User.builder()
                .username("regular_customer")
                .email("customer@dongly.vn")
                .passwordHash(passwordEncoder.encode("CustomerPass123"))
                .fullName("Khách hàng Thân thiết")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>())
                .build());

        adminToken = jwtTokenProvider.generateAccessToken(
                adminUser.getId(),
                adminUser.getUsername(),
                adminUser.getEmail(),
                List.of("ROLE_ADMIN"),
                List.of("USER_READ", "USER_CREATE", "USER_UPDATE", "ROLE_ASSIGN", "SECRET_ACTION")
        );

        // Operator có ROLE_ASSIGN nhưng KHÔNG có ROLE_ADMIN và KHÔNG có SECRET_ACTION
        User operatorUser = userRepository.save(User.builder()
                .username("operator_member")
                .email("operator@dongly.vn")
                .passwordHash(passwordEncoder.encode("OpPass123"))
                .fullName("Điều hành viên")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build());

        operatorToken = jwtTokenProvider.generateAccessToken(
                operatorUser.getId(),
                operatorUser.getUsername(),
                operatorUser.getEmail(),
                List.of("ROLE_OPERATOR"),
                List.of("USER_READ", "USER_CREATE", "USER_UPDATE", "ROLE_ASSIGN")
        );

        customerToken = jwtTokenProvider.generateAccessToken(
                customerUser.getId(),
                customerUser.getUsername(),
                customerUser.getEmail(),
                List.of("ROLE_CUSTOMER"),
                List.of()
        );
    }

    @Test
    @DisplayName("Lấy danh sách người dùng thành công với quyền USER_READ")
    void getUsers_success() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.pagination.totalElements", is(4)));
    }

    @Test
    @DisplayName("Tạo người dùng mới thành công: mã hóa mật khẩu, không để lộ mật khẩu trong response")
    void createUser_success() throws Exception {
        String payload = """
                {
                    "username": "driver_haiphong",
                    "email": "driver_hp@dongly.vn",
                    "password": "DriverSecret123",
                    "fullName": "Tài xế Hải Phòng",
                    "phone": "0988123456",
                    "roleCodes": ["STAFF"]
                }
                """;

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is("driver_haiphong")))
                .andExpect(jsonPath("$.data.email", is("driver_hp@dongly.vn")))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        User savedUser = userRepository.findByUsername("driver_haiphong").orElseThrow();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("DriverSecret123");
        assertThat(passwordEncoder.matches("DriverSecret123", savedUser.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Tạo người dùng trùng username trả về lỗi xung đột 409")
    void createUser_duplicateUsername_returns409() throws Exception {
        String payload = """
                {
                    "username": "system_admin",
                    "email": "another@dongly.vn",
                    "password": "Password123",
                    "fullName": "Trùng Admin",
                    "phone": "0988123456"
                }
                """;

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("RESOURCE_ALREADY_EXISTS")));
    }

    @Test
    @DisplayName("Cập nhật thông tin người dùng thành công")
    void updateUser_success() throws Exception {
        String updatePayload = """
                {
                    "fullName": "Quản trị hệ thống (Đã cập nhật)",
                    "email": "sysadmin_updated@dongly.vn",
                    "phone": "0912999888",
                    "roleCodes": ["ADMIN"]
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + adminUser.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fullName", is("Quản trị hệ thống (Đã cập nhật)")))
                .andExpect(jsonPath("$.data.email", is("sysadmin_updated@dongly.vn")));
    }

    @Test
    @DisplayName("Thay đổi trạng thái tài khoản thành công")
    void updateUserStatus_success() throws Exception {
        User staff = userRepository.save(User.builder()
                .username("staff_temp")
                .email("staff_temp@dongly.vn")
                .passwordHash(passwordEncoder.encode("StaffPass123"))
                .fullName("Nhân viên tạm thời")
                .status(UserStatus.ACTIVE)
                .build());

        String statusPayload = """
                {
                    "status": "LOCKED"
                }
                """;

        mockMvc.perform(patch("/api/v1/users/" + staff.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("LOCKED")));
    }

    @Test
    @DisplayName("Quản trị viên không thể tự khóa tài khoản của chính mình")
    void updateUserStatus_adminSelfLock_returns422() throws Exception {
        String statusPayload = """
                {
                    "status": "LOCKED"
                }
                """;

        mockMvc.perform(patch("/api/v1/users/" + adminUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusPayload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("BUSINESS_RULE_VIOLATION")));
    }

    // ==========================================
    // TESTS CHO GET & PUT /api/v1/users/{id}/roles
    // ==========================================

    @Test
    @DisplayName("GET /api/v1/users/{id}/roles: Chưa xác thực trả về 401 Unauthorized")
    void getUserRoles_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + staffUser.getId() + "/roles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id}/roles: Khách hàng không có quyền xem vai trò người khác trả về 403")
    void getUserRoles_forbidden_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + staffUser.getId() + "/roles")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id}/roles: Người dùng xem vai trò của chính mình thành công")
    void getUserRoles_selfAccess_success() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + customerUser.getId() + "/roles")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/users/{id}/roles: Admin xem vai trò của người dùng thành công")
    void getUserRoles_adminAccess_success() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + staffUser.getId() + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[*].code", hasItem("STAFF")));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/roles: Chưa xác thực trả về 401 Unauthorized")
    void updateUserRoles_unauthenticated_returns401() throws Exception {
        String payload = """
                {
                    "roleCodes": ["STAFF"]
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + customerUser.getId() + "/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/roles: Thiếu quyền ROLE_ASSIGN trả về 403 Forbidden")
    void updateUserRoles_forbidden_returns403() throws Exception {
        String payload = """
                {
                    "roleCodes": ["STAFF"]
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + customerUser.getId() + "/roles")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/roles: Gán role không tồn tại trả về 404 Not Found")
    void updateUserRoles_nonExistentRole_returns404() throws Exception {
        String payload = """
                {
                    "roleCodes": ["NON_EXISTENT_ROLE_CODE_999"]
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + customerUser.getId() + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/roles: Gán vai trò đang bị INACTIVE trả về 422 Unprocessable Entity")
    void updateUserRoles_inactiveRole_returns422() throws Exception {
        String payload = """
                {
                    "roleCodes": ["INACTIVE_ROLE"]
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + customerUser.getId() + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("BUSINESS_RULE_VIOLATION")));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/roles: Ngăn tự nâng quyền - Operator không được phân bổ vai trò ADMIN")
    void updateUserRoles_privilegeEscalation_assignAdmin_returns403() throws Exception {
        String payload = """
                {
                    "roleCodes": ["ADMIN"]
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + customerUser.getId() + "/roles")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/roles: Ngăn tự nâng quyền - Operator không được gán role chứa quyền mà mình không sở hữu")
    void updateUserRoles_privilegeEscalation_unpossessedPerm_returns403() throws Exception {
        // SECRET_ROLE chứa SECRET_ACTION mà operator không sở hữu
        String payload = """
                {
                    "roleCodes": ["SECRET_ROLE"]
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + customerUser.getId() + "/roles")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/roles: Ngăn tự nâng quyền - Không được tự thay đổi vai trò của chính mình")
    void updateUserRoles_privilegeEscalation_selfAssign_returns403() throws Exception {
        User operatorUser = userRepository.findByUsername("operator_member").orElseThrow();

        String payload = """
                {
                    "roleCodes": ["STAFF"]
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + operatorUser.getId() + "/roles")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/roles: Không thể thu hồi vai trò ADMIN của tài khoản quản trị cuối cùng")
    void updateUserRoles_stripLastAdmin_returns422() throws Exception {
        // adminUser là admin duy nhất trong hệ thống, cố tình hạ xuống STAFF
        String payload = """
                {
                    "roleCodes": ["STAFF"]
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + adminUser.getId() + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("BUSINESS_RULE_VIOLATION")));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/roles: Admin cập nhật danh sách vai trò cho người dùng thành công")
    void updateUserRoles_admin_success() throws Exception {
        String payload = """
                {
                    "roleCodes": ["STAFF"]
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + customerUser.getId() + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[*].code", hasItem("STAFF")));

        User reloadedUser = userRepository.findById(customerUser.getId()).orElseThrow();
        assertThat(reloadedUser.getRoles()).hasSize(1);
        assertThat(reloadedUser.getRoles().iterator().next().getCode()).isEqualTo("STAFF");
    }

    @Test
    @DisplayName("POST /api/v1/users: Ngăn tự nâng quyền - Operator không được tạo user có vai trò ADMIN")
    void createUser_privilegeEscalation_assignAdmin_returns403() throws Exception {
        String payload = """
                {
                    "username": "escalated_user",
                    "email": "escalated@dongly.vn",
                    "password": "Password123",
                    "fullName": "Leo Thang",
                    "roleCodes": ["ADMIN"]
                }
                """;

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}: Ngăn tự nâng quyền - Operator không được gán vai trò ADMIN khi cập nhật user")
    void updateUser_privilegeEscalation_assignAdmin_returns403() throws Exception {
        String payload = """
                {
                    "fullName": "Nhân viên nâng quyền",
                    "email": "staff_escalated@dongly.vn",
                    "roleCodes": ["ADMIN"]
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + staffUser.getId())
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}: Operator không được chỉnh sửa thông tin của tài khoản ADMIN")
    void updateUser_nonAdminEditAdminProfile_returns403() throws Exception {
        String payload = """
                {
                    "fullName": "Cố tình sửa Admin",
                    "email": "hacked_admin@dongly.vn"
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + adminUser.getId())
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id}/status: Operator không được khóa tài khoản ADMIN")
    void updateUserStatus_nonAdminLockAdmin_returns403() throws Exception {
        String payload = """
                {
                    "status": "LOCKED"
                }
                """;

        mockMvc.perform(patch("/api/v1/users/" + adminUser.getId() + "/status")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/roles: Cập nhật vai trò sẽ tự động thu hồi toàn bộ Refresh Token của user")
    void updateUserRoles_revokesRefreshTokens() throws Exception {
        // Tạo sẵn Refresh Token cho customerUser
        RefreshToken rt = refreshTokenRepository.save(RefreshToken.builder()
                .user(customerUser)
                .tokenHash("hash_sample_token_123")
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .createdByIp("127.0.0.1")
                .build());

        assertThat(rt.isRevoked()).isFalse();

        String payload = """
                {
                    "roleCodes": ["STAFF"]
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + customerUser.getId() + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        // Kiểm tra Refresh Token đã bị thu hồi hoàn toàn
        RefreshToken reloadedRt = refreshTokenRepository.findById(rt.getId()).orElseThrow();
        assertThat(reloadedRt.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/roles: Request bị từ chối không làm thay đổi dữ liệu trong DB (Rollback)")
    void updateUserRoles_rejectedRequest_doesNotChangeDatabase() throws Exception {
        String payload = """
                {
                    "roleCodes": ["NON_EXISTENT_ROLE_XYZ"]
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + staffUser.getId() + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));

        // Kiểm tra vai trò của staffUser trong DB vẫn nguyên vẹn là STAFF
        User reloadedStaff = userRepository.findById(staffUser.getId()).orElseThrow();
        assertThat(reloadedStaff.getRoles()).hasSize(1);
        assertThat(reloadedStaff.getRoles().iterator().next().getCode()).isEqualTo("STAFF");
    }

    @Test
    @DisplayName("GET /api/v1/users: Request kèm Access Token đã hết hạn trả về 401 Unauthorized")
    void requestWithExpiredToken_returns401() throws Exception {
        javax.crypto.SecretKey key = Keys.hmacShaKeyFor(
                "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RpbmctcHVycG9zZXMtb25seS1tdXN0LWJlLTI1Ni1iaXRz"
                        .getBytes(StandardCharsets.UTF_8));

        String expiredToken = Jwts.builder()
                .subject("expired_user@dongly.vn")
                .claim("uid", UUID.randomUUID().toString())
                .claim("username", "expired_user")
                .claim("roles", List.of("ROLE_ADMIN"))
                .claim("permissions", List.of("USER_READ"))
                .issuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
                .expiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }
}
