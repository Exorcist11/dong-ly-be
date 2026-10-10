package com.dongly.modules.user;

import com.dongly.modules.user.entity.Permission;
import com.dongly.modules.user.entity.Role;
import com.dongly.modules.user.entity.RoleStatus;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PermissionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String tokenWithPermissionRead;
    private String tokenWithRoleRead;
    private String tokenWithoutCatalogPermission;

    @BeforeEach
    void setUp() {
        Permission permRead = permissionRepository.findByCode("PERMISSION_READ")
                .orElseGet(() -> permissionRepository.save(Permission.builder()
                        .code("PERMISSION_READ")
                        .name("Xem danh mục quyền")
                        .description("Quyền xem danh mục quyền")
                        .module("PERMISSION")
                        .action("READ")
                        .build()));

        Permission roleRead = permissionRepository.findByCode("ROLE_READ")
                .orElseGet(() -> permissionRepository.save(Permission.builder()
                        .code("ROLE_READ")
                        .name("Xem vai trò")
                        .description("Quyền xem danh sách vai trò")
                        .module("ROLE")
                        .action("READ")
                        .build()));

        Permission userRead = permissionRepository.findByCode("USER_READ")
                .orElseGet(() -> permissionRepository.save(Permission.builder()
                        .code("USER_READ")
                        .name("Xem người dùng")
                        .description("Quyền xem người dùng")
                        .module("USER")
                        .action("READ")
                        .build()));

        // Role 1: Có quyền PERMISSION_READ
        Role permReadRole = roleRepository.save(Role.builder()
                .code("ROLE_TEST_PERM_READ")
                .name("Test Role with PERMISSION_READ")
                .status(RoleStatus.ACTIVE)
                .permissions(Set.of(permRead))
                .build());

        // Role 2: Có quyền ROLE_READ
        Role roleReadRole = roleRepository.save(Role.builder()
                .code("ROLE_TEST_ROLE_READ")
                .name("Test Role with ROLE_READ")
                .status(RoleStatus.ACTIVE)
                .permissions(Set.of(roleRead))
                .build());

        // Role 3: Chỉ có quyền USER_READ (không có PERMISSION_READ hoặc ROLE_READ)
        Role userReadRole = roleRepository.save(Role.builder()
                .code("ROLE_TEST_USER_ONLY")
                .name("Test Role with USER_READ only")
                .status(RoleStatus.ACTIVE)
                .permissions(Set.of(userRead))
                .build());

        User userWithPermRead = userRepository.save(User.builder()
                .username("user_perm_read")
                .email("perm_read@dongly.vn")
                .passwordHash("hashed")
                .fullName("User with PERMISSION_READ")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(permReadRole))
                .build());

        User userWithRoleRead = userRepository.save(User.builder()
                .username("user_role_read")
                .email("role_read@dongly.vn")
                .passwordHash("hashed")
                .fullName("User with ROLE_READ")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(roleReadRole))
                .build());

        User userWithoutCatalogPerm = userRepository.save(User.builder()
                .username("user_user_only")
                .email("user_only@dongly.vn")
                .passwordHash("hashed")
                .fullName("User with USER_READ only")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(userReadRole))
                .build());

        tokenWithPermissionRead = jwtTokenProvider.generateAccessToken(
                userWithPermRead.getId(),
                userWithPermRead.getUsername(),
                userWithPermRead.getEmail(),
                List.of(permReadRole.getCode()),
                List.of("PERMISSION_READ")
        );

        tokenWithRoleRead = jwtTokenProvider.generateAccessToken(
                userWithRoleRead.getId(),
                userWithRoleRead.getUsername(),
                userWithRoleRead.getEmail(),
                List.of(roleReadRole.getCode()),
                List.of("ROLE_READ")
        );

        tokenWithoutCatalogPermission = jwtTokenProvider.generateAccessToken(
                userWithoutCatalogPerm.getId(),
                userWithoutCatalogPerm.getUsername(),
                userWithoutCatalogPerm.getEmail(),
                List.of(userReadRole.getCode()),
                List.of("USER_READ")
        );
    }

    @Test
    @DisplayName("GET /api/v1/permissions: Chưa xác thực (thiếu Bearer token) trả về 401 Unauthorized")
    void getPermissions_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/permissions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("GET /api/v1/permissions: Đã xác thực nhưng thiếu PERMISSION_READ/ROLE_READ trả về 403 Forbidden")
    void getPermissions_forbidden_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/permissions")
                        .header("Authorization", "Bearer " + tokenWithoutCatalogPermission)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("GET /api/v1/permissions: Có quyền PERMISSION_READ trả về 200 OK và cấu trúc danh mục đầy đủ")
    void getPermissions_withPermissionRead_returns200AndValidStructure() throws Exception {
        mockMvc.perform(get("/api/v1/permissions")
                        .header("Authorization", "Bearer " + tokenWithPermissionRead)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Lấy danh mục quyền hạn thành công")))
                .andExpect(jsonPath("$.data.totalPermissions", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.modules").isArray())
                .andExpect(jsonPath("$.data.permissions").isArray())
                .andExpect(jsonPath("$.data.permissions[*].code", hasItem("PERMISSION_READ")))
                .andExpect(jsonPath("$.data.permissions[0].code").exists())
                .andExpect(jsonPath("$.data.permissions[0].name").exists())
                .andExpect(jsonPath("$.data.permissions[0].module").exists())
                .andExpect(jsonPath("$.data.modules[0].module").exists())
                .andExpect(jsonPath("$.data.modules[0].permissions").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/permissions: Có quyền ROLE_READ trả về 200 OK thành công")
    void getPermissions_withRoleRead_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/permissions")
                        .header("Authorization", "Bearer " + tokenWithRoleRead)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalPermissions", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("GET /api/v1/permissions?module=USER: Lọc chính xác danh mục theo module")
    void getPermissions_withModuleFilter_returnsFilteredPermissions() throws Exception {
        mockMvc.perform(get("/api/v1/permissions")
                        .param("module", "USER")
                        .header("Authorization", "Bearer " + tokenWithPermissionRead)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.permissions[*].module", everyItem(is("USER"))))
                .andExpect(jsonPath("$.data.modules[*].module", everyItem(is("USER"))));
    }
}
