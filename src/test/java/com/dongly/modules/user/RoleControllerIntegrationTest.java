package com.dongly.modules.user;

import com.dongly.modules.user.dto.AssignRolePermissionsRequest;
import com.dongly.modules.user.dto.CreateRoleRequest;
import com.dongly.modules.user.dto.UpdateRoleRequest;
import com.dongly.modules.user.dto.UpdateRoleStatusRequest;
import com.dongly.modules.user.entity.Permission;
import com.dongly.modules.user.entity.Role;
import com.dongly.modules.user.entity.RoleStatus;
import com.dongly.modules.user.entity.User;
import com.dongly.modules.user.entity.UserStatus;
import com.dongly.modules.user.repository.PermissionRepository;
import com.dongly.modules.user.repository.RoleRepository;
import com.dongly.modules.user.repository.UserRepository;
import com.dongly.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RoleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String operatorToken;
    private Role systemAdminRole;
    private Role customRole;
    private Permission roleReadPerm;
    private Permission roleCreatePerm;
    private Permission roleUpdatePerm;
    private Permission roleDeletePerm;
    private Permission roleAssignPerm;

    @BeforeEach
    void setUp() {
        roleReadPerm = permissionRepository.findByCode("ROLE_READ")
                .orElseGet(() -> permissionRepository.save(Permission.builder()
                        .code("ROLE_READ")
                        .name("Xem vai trò")
                        .module("ROLE")
                        .action("READ")
                        .build()));

        roleCreatePerm = permissionRepository.findByCode("ROLE_CREATE")
                .orElseGet(() -> permissionRepository.save(Permission.builder()
                        .code("ROLE_CREATE")
                        .name("Tạo vai trò")
                        .module("ROLE")
                        .action("CREATE")
                        .build()));

        roleUpdatePerm = permissionRepository.findByCode("ROLE_UPDATE")
                .orElseGet(() -> permissionRepository.save(Permission.builder()
                        .code("ROLE_UPDATE")
                        .name("Cập nhật vai trò")
                        .module("ROLE")
                        .action("UPDATE")
                        .build()));

        roleDeletePerm = permissionRepository.findByCode("ROLE_DELETE")
                .orElseGet(() -> permissionRepository.save(Permission.builder()
                        .code("ROLE_DELETE")
                        .name("Vô hiệu hóa/Xóa vai trò")
                        .module("ROLE")
                        .action("DELETE")
                        .build()));

        roleAssignPerm = permissionRepository.findByCode("ROLE_ASSIGN")
                .orElseGet(() -> permissionRepository.save(Permission.builder()
                        .code("ROLE_ASSIGN")
                        .name("Gán quyền vai trò")
                        .module("ROLE")
                        .action("ASSIGN")
                        .build()));

        systemAdminRole = roleRepository.findByCode("ADMIN")
                .map(role -> {
                    role.setSystem(true);
                    role.setStatus(RoleStatus.ACTIVE);
                    Set<Permission> perms = new HashSet<>(role.getPermissions());
                    perms.addAll(List.of(roleReadPerm, roleCreatePerm, roleUpdatePerm, roleDeletePerm, roleAssignPerm));
                    role.setPermissions(perms);
                    return roleRepository.save(role);
                })
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .code("ADMIN")
                        .name("Quản trị viên")
                        .isSystem(true)
                        .status(RoleStatus.ACTIVE)
                        .permissions(new HashSet<>(Set.of(roleReadPerm, roleCreatePerm, roleUpdatePerm, roleDeletePerm, roleAssignPerm)))
                        .build()));

        customRole = roleRepository.findByCode("CUSTOM_TEST_ROLE")
                .map(role -> {
                    role.setStatus(RoleStatus.ACTIVE);
                    role.setName("Vai trò tùy chỉnh");
                    role.setDescription("Dùng cho test");
                    role.setSystem(false);
                    role.setPermissions(new HashSet<>(Set.of(roleReadPerm)));
                    return roleRepository.save(role);
                })
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .code("CUSTOM_TEST_ROLE")
                        .name("Vai trò tùy chỉnh")
                        .description("Dùng cho test")
                        .isSystem(false)
                        .status(RoleStatus.ACTIVE)
                        .permissions(new HashSet<>(Set.of(roleReadPerm)))
                        .build()));

        String adminSuffix = UUID.randomUUID().toString().substring(0, 8);
        User adminUser = userRepository.save(User.builder()
                .username("adm_" + adminSuffix)
                .email("adm_" + adminSuffix + "@dongly.vn")
                .passwordHash("hashed")
                .fullName("Quản trị viên Test")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(systemAdminRole)))
                .build());

        // Token có đầy đủ quyền ROLE_*
        adminToken = jwtTokenProvider.generateAccessToken(
                adminUser.getId(),
                adminUser.getUsername(),
                adminUser.getEmail(),
                List.of(systemAdminRole.getCode()),
                List.of("ROLE_READ", "ROLE_CREATE", "ROLE_UPDATE", "ROLE_DELETE", "ROLE_ASSIGN", "PERMISSION_READ")
        );

        // Token chỉ có quyền ROLE_READ
        operatorToken = jwtTokenProvider.generateAccessToken(
                UUID.randomUUID(),
                "operator_user",
                "operator@dongly.vn",
                List.of("OPERATOR"),
                List.of("ROLE_READ")
        );
    }

    @Test
    @DisplayName("GET /api/v1/roles: Chưa xác thực trả về 401")
    void getRoles_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("GET /api/v1/roles: Thành công với phân trang, tìm kiếm và lọc trạng thái")
    void getRoles_success_withPaginationSearchAndFilter() throws Exception {
        mockMvc.perform(get("/api/v1/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "ADMIN")
                        .param("status", "ACTIVE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items[*].code", hasItem("ADMIN")))
                .andExpect(jsonPath("$.data.pagination.page", is(0)));
    }

    @Test
    @DisplayName("GET /api/v1/roles/{id}: Xem chi tiết vai trò kèm toàn bộ quyền hạn")
    void getRoleById_success() throws Exception {
        mockMvc.perform(get("/api/v1/roles/" + customRole.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.code", is("CUSTOM_TEST_ROLE")))
                .andExpect(jsonPath("$.data.isSystem", is(false)))
                .andExpect(jsonPath("$.data.permissions[*].code", hasItem("ROLE_READ")));
    }

    @Test
    @DisplayName("POST /api/v1/roles: Thiếu quyền ROLE_CREATE trả về 403 Forbidden")
    void createRole_forbidden_returns403() throws Exception {
        CreateRoleRequest request = new CreateRoleRequest("NEW_ROLE", "Role Mới", "Mô tả", null);

        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("POST /api/v1/roles: Tạo mới thành công vai trò kèm quyền ban đầu")
    void createRole_success() throws Exception {
        CreateRoleRequest request = new CreateRoleRequest(
                "DISPATCHER",
                "Điều độ xe",
                "Phân công phương tiện và tài xế",
                Set.of("ROLE_READ")
        );

        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.code", is("DISPATCHER")))
                .andExpect(jsonPath("$.data.isSystem", is(false)))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.permissions[*].code", hasItem("ROLE_READ")));
    }

    @Test
    @DisplayName("POST /api/v1/roles: Trùng mã vai trò (duplicate code) trả về 409 Conflict")
    void createRole_duplicateCode_returns409() throws Exception {
        CreateRoleRequest request = new CreateRoleRequest(
                "CUSTOM_TEST_ROLE",
                "Trùng mã",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("RESOURCE_ALREADY_EXISTS")));
    }

    @Test
    @DisplayName("POST /api/v1/roles: Gán permission không tồn tại ném lỗi 404 Not Found")
    void createRole_nonExistentPermission_returns404() throws Exception {
        CreateRoleRequest request = new CreateRoleRequest(
                "INVALID_PERM_ROLE",
                "Sai quyền",
                null,
                Set.of("UNKNOWN_PERMISSION_123")
        );

        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("PUT /api/v1/roles/{id}: Cập nhật tên và mô tả vai trò thành công")
    void updateRole_success() throws Exception {
        UpdateRoleRequest request = new UpdateRoleRequest("Tên vai trò mới", "Mô tả mới");

        mockMvc.perform(put("/api/v1/roles/" + customRole.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Tên vai trò mới")))
                .andExpect(jsonPath("$.data.description", is("Mô tả mới")));
    }

    @Test
    @DisplayName("PATCH /api/v1/roles/{id}/status: Vô hiệu hóa vai trò hệ thống bị từ chối 403")
    void updateRoleStatus_systemRole_returns403() throws Exception {
        UpdateRoleStatusRequest request = new UpdateRoleStatusRequest(RoleStatus.INACTIVE);

        mockMvc.perform(patch("/api/v1/roles/" + systemAdminRole.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("SYSTEM_ROLE_PROTECTED")));
    }

    @Test
    @DisplayName("PATCH /api/v1/roles/{id}/status: Vô hiệu hóa vai trò đang có người dùng gán bị từ chối 409")
    void updateRoleStatus_roleInUse_returns409() throws Exception {
        // Gán customRole cho một user
        String uSuffix = UUID.randomUUID().toString().substring(0, 8);
        userRepository.save(User.builder()
                .username("u_inuse_" + uSuffix)
                .email("u_inuse_" + uSuffix + "@dongly.vn")
                .passwordHash("hash")
                .fullName("User Assigned Role")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(customRole)))
                .build());

        UpdateRoleStatusRequest request = new UpdateRoleStatusRequest(RoleStatus.INACTIVE);

        mockMvc.perform(patch("/api/v1/roles/" + customRole.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("ROLE_IN_USE")));
    }

    @Test
    @DisplayName("PATCH /api/v1/roles/{id}/status: Vô hiệu hóa vai trò tùy chỉnh chưa gán thành công")
    void updateRoleStatus_customRole_success() throws Exception {
        UpdateRoleStatusRequest request = new UpdateRoleStatusRequest(RoleStatus.INACTIVE);

        mockMvc.perform(patch("/api/v1/roles/" + customRole.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("INACTIVE")));
    }

    @Test
    @DisplayName("GET /api/v1/roles/{id}/permissions: Lấy danh sách quyền của vai trò")
    void getRolePermissions_success() throws Exception {
        mockMvc.perform(get("/api/v1/roles/" + customRole.getId() + "/permissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].code", hasItem("ROLE_READ")));
    }

    @Test
    @DisplayName("PUT /api/v1/roles/{id}/permissions: Cố tình thu hồi quyền cốt lõi của ADMIN bị từ chối 422")
    void assignRolePermissions_stripAdminCorePermissions_returns422() throws Exception {
        // Cố xóa hết quyền của ADMIN
        AssignRolePermissionsRequest request = new AssignRolePermissionsRequest(Set.of());

        mockMvc.perform(put("/api/v1/roles/" + systemAdminRole.getId() + "/permissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("BUSINESS_RULE_VIOLATION")));
    }

    @Test
    @DisplayName("PUT /api/v1/roles/{id}/permissions: Gán permission không tồn tại ném 404 và rollback")
    void assignRolePermissions_nonExistentPermission_returns404AndRollback() throws Exception {
        AssignRolePermissionsRequest request = new AssignRolePermissionsRequest(
                Set.of("ROLE_READ", "NON_EXISTENT_PERM")
        );

        mockMvc.perform(put("/api/v1/roles/" + customRole.getId() + "/permissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));

        // Kiểm tra dữ liệu sau lỗi: customRole vẫn giữ nguyên danh sách quyền cũ (Rollback)
        Role reloadedRole = roleRepository.findWithPermissionsById(customRole.getId()).orElseThrow();
        assertEquals(1, reloadedRole.getPermissions().size());
        assertTrue(reloadedRole.getPermissions().stream().anyMatch(p -> "ROLE_READ".equals(p.getCode())));
    }

    @Test
    @DisplayName("PUT /api/v1/roles/{id}/permissions: Cập nhật ma trận quyền vai trò tùy chỉnh thành công")
    void assignRolePermissions_customRole_success() throws Exception {
        AssignRolePermissionsRequest request = new AssignRolePermissionsRequest(
                Set.of("ROLE_READ", "ROLE_CREATE")
        );

        mockMvc.perform(put("/api/v1/roles/" + customRole.getId() + "/permissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.permissions[*].code", hasItem("ROLE_CREATE")));
    }

    @Test
    @DisplayName("DELETE /api/v1/roles/{id}: Thiếu quyền ROLE_DELETE bị từ chối 403 Forbidden")
    void deleteRole_forbidden_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/roles/" + customRole.getId())
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("DELETE /api/v1/roles/{id}: Xóa vai trò hệ thống bị từ chối 403")
    void deleteRole_systemRole_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/roles/" + systemAdminRole.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("SYSTEM_ROLE_PROTECTED")));
    }

    @Test
    @DisplayName("DELETE /api/v1/roles/{id}: Xóa vai trò đang có người dùng gán bị từ chối 409")
    void deleteRole_roleInUse_returns409() throws Exception {
        String delSuffix = UUID.randomUUID().toString().substring(0, 8);
        userRepository.save(User.builder()
                .username("u_del_" + delSuffix)
                .email("u_del_" + delSuffix + "@dongly.vn")
                .passwordHash("hash")
                .fullName("User In Use")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(customRole)))
                .build());

        mockMvc.perform(delete("/api/v1/roles/" + customRole.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("ROLE_IN_USE")));
    }

    @Test
    @DisplayName("DELETE /api/v1/roles/{id}: Xóa vai trò tùy chỉnh chưa gán thành công")
    void deleteRole_customRole_success() throws Exception {
        Role deletableRole = roleRepository.save(Role.builder()
                .code("ROLE_TO_DELETE")
                .name("Vai trò để xóa")
                .isSystem(false)
                .status(RoleStatus.ACTIVE)
                .permissions(new HashSet<>())
                .build());

        mockMvc.perform(delete("/api/v1/roles/" + deletableRole.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        assertFalse(roleRepository.existsById(deletableRole.getId()));
    }

    @Test
    @DisplayName("PUT /api/v1/roles/{id}/permissions: Ngăn tự nâng quyền khi người dùng không sở hữu quyền được gán")
    void assignRolePermissions_privilegeEscalation_returns403() throws Exception {
        // Tạo token của một user chỉ có ROLE_ASSIGN nhưng KHÔNG có ROLE_CREATE
        String roleAssignerToken = jwtTokenProvider.generateAccessToken(
                UUID.randomUUID(),
                "assigner_user",
                "assigner@dongly.vn",
                List.of("OPERATOR"),
                List.of("ROLE_ASSIGN")
        );

        // Assigner cố gán ROLE_CREATE cho customRole
        AssignRolePermissionsRequest request = new AssignRolePermissionsRequest(Set.of("ROLE_CREATE"));

        mockMvc.perform(put("/api/v1/roles/" + customRole.getId() + "/permissions")
                        .header("Authorization", "Bearer " + roleAssignerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }
}
