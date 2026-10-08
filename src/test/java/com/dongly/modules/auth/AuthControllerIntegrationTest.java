package com.dongly.modules.auth;

import com.dongly.modules.auth.repository.RefreshTokenRepository;
import com.dongly.modules.user.entity.Permission;
import com.dongly.modules.user.entity.Role;
import com.dongly.modules.user.entity.User;
import com.dongly.modules.user.entity.UserStatus;
import com.dongly.modules.user.repository.PermissionRepository;
import com.dongly.modules.user.repository.RoleRepository;
import com.dongly.modules.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        Permission permRead = permissionRepository.save(Permission.builder()
                .code("USER_READ")
                .name("Xem người dùng")
                .build());

        Role adminRole = roleRepository.save(Role.builder()
                .code("ADMIN")
                .name("Quản trị viên")
                .permissions(Set.of(permRead))
                .build());

        testUser = userRepository.save(User.builder()
                .username("test_admin")
                .email("test_admin@dongly.vn")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .fullName("Quản trị viên Test")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());
    }

    @Test
    @DisplayName("Chuỗi luồng xác thực toàn diện: Login chỉ trả về token -> /auth/me lấy user -> Refresh xoay vòng -> Logout")
    void fullAuthenticationFlow() throws Exception {
        // 1. Đăng nhập: POST /api/v1/auth/login
        String loginPayload = """
                {
                    "username": "test_admin",
                    "password": "Password@123"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.expiresIn").isNumber())
                // QUAN TRỌNG: Xác minh tuyệt đối không trả về thông tin user trong response đăng nhập
                .andExpect(jsonPath("$.data.user").doesNotExist())
                .andReturn();

        JsonNode loginData = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("data");
        String accessToken = loginData.get("accessToken").asText();
        String refreshToken = loginData.get("refreshToken").asText();

        // 2. Lấy thông tin người dùng: GET /api/v1/auth/me sử dụng Bearer accessToken
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is("test_admin")))
                .andExpect(jsonPath("$.data.email", is("test_admin@dongly.vn")))
                .andExpect(jsonPath("$.data.roles[0]", is("ADMIN")))
                .andExpect(jsonPath("$.data.permissions[0]", is("USER_READ")));

        // 3. Làm mới token: POST /api/v1/auth/refresh với refreshToken
        String refreshPayload = String.format("{\"refreshToken\":\"%s\"}", refreshToken);
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andReturn();

        JsonNode refreshData = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).get("data");
        String newAccessToken = refreshData.get("accessToken").asText();
        String newRefreshToken = refreshData.get("refreshToken").asText();

        // 4. Token mới hoạt động bình thường trên /auth/me
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username", is("test_admin")));

        // 5. Đăng xuất: POST /api/v1/auth/logout với newRefreshToken
        String logoutPayload = String.format("{\"refreshToken\":\"%s\"}", newRefreshToken);
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + newAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 6. Cố gắng sử dụng lại refreshToken đã bị thu hồi -> Bị từ chối 401
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_TOKEN")));
    }
}
