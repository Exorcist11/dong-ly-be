package com.dongly.modules.user;

import com.dongly.modules.auth.repository.RefreshTokenRepository;
import com.dongly.modules.user.entity.Permission;
import com.dongly.modules.user.entity.Role;
import com.dongly.modules.user.entity.User;
import com.dongly.modules.user.entity.UserStatus;
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

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
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
    private String adminToken;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        Permission pRead = permissionRepository.save(Permission.builder().code("USER_READ").name("Xem").build());
        Permission pCreate = permissionRepository.save(Permission.builder().code("USER_CREATE").name("Tạo").build());
        Permission pUpdate = permissionRepository.save(Permission.builder().code("USER_UPDATE").name("Sửa").build());

        Role adminRole = roleRepository.save(Role.builder()
                .code("ADMIN")
                .name("Quản trị viên")
                .permissions(Set.of(pRead, pCreate, pUpdate))
                .build());

        Role staffRole = roleRepository.save(Role.builder()
                .code("STAFF")
                .name("Nhân viên")
                .build());

        adminUser = userRepository.save(User.builder()
                .username("system_admin")
                .email("sysadmin@dongly.vn")
                .passwordHash(passwordEncoder.encode("AdminPass123"))
                .fullName("Quản trị hệ thống")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        adminToken = jwtTokenProvider.generateAccessToken(
                adminUser.getId(),
                adminUser.getUsername(),
                adminUser.getEmail(),
                List.of("ROLE_ADMIN"),
                List.of("USER_READ", "USER_CREATE", "USER_UPDATE")
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
                .andExpect(jsonPath("$.data.pagination.totalElements", is(1)));
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

        // Kiểm tra cơ sở dữ liệu: mật khẩu đã được mã hóa an toàn
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
}
