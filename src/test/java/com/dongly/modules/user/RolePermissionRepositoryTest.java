package com.dongly.modules.user;

import com.dongly.modules.user.entity.Permission;
import com.dongly.modules.user.entity.Role;
import com.dongly.modules.user.entity.RoleStatus;
import com.dongly.modules.user.entity.User;
import com.dongly.modules.user.entity.UserStatus;
import com.dongly.modules.user.repository.PermissionRepository;
import com.dongly.modules.user.repository.RoleRepository;
import com.dongly.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RolePermissionRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRepository userRepository;

    private Permission permRead;
    private Permission permCreate;
    private Role adminRole;
    private Role customRole;

    @BeforeEach
    void setUp() {
        permRead = permissionRepository.save(Permission.builder()
                .code("TEST_USER_READ")
                .name("Test User Read")
                .description("Test Description Read")
                .module("USER")
                .action("READ")
                .build());

        permCreate = permissionRepository.save(Permission.builder()
                .code("TEST_USER_CREATE")
                .name("Test User Create")
                .description("Test Description Create")
                .module("USER")
                .action("CREATE")
                .build());

        adminRole = roleRepository.save(Role.builder()
                .code("TEST_ADMIN")
                .name("Test Admin")
                .description("Test Admin Role")
                .status(RoleStatus.ACTIVE)
                .isSystem(true)
                .permissions(Set.of(permRead, permCreate))
                .build());

        customRole = roleRepository.save(Role.builder()
                .code("TEST_CUSTOM")
                .name("Test Custom")
                .description("Test Custom Role")
                .status(RoleStatus.INACTIVE)
                .isSystem(false)
                .build());
    }

    @Test
    @DisplayName("Kiểm tra ánh xạ thực thể Role: status, isSystem, isActive và permissions")
    void roleEntityMapping_success() {
        Role found = roleRepository.findByCode("TEST_ADMIN").orElseThrow();

        assertThat(found.getCode()).isEqualTo("TEST_ADMIN");
        assertThat(found.getStatus()).isEqualTo(RoleStatus.ACTIVE);
        assertThat(found.isActive()).isTrue();
        assertThat(found.isSystem()).isTrue();
        assertThat(found.getPermissions()).hasSize(2);

        Role inactiveRole = roleRepository.findByCode("TEST_CUSTOM").orElseThrow();
        assertThat(inactiveRole.getStatus()).isEqualTo(RoleStatus.INACTIVE);
        assertThat(inactiveRole.isActive()).isFalse();
        assertThat(inactiveRole.isSystem()).isFalse();
    }

    @Test
    @DisplayName("Kiểm tra ánh xạ thực thể Permission: module, action và giá trị mặc định")
    void permissionEntityMapping_success() {
        Permission found = permissionRepository.findByCode("TEST_USER_READ").orElseThrow();

        assertThat(found.getCode()).isEqualTo("TEST_USER_READ");
        assertThat(found.getModule()).isEqualTo("USER");
        assertThat(found.getAction()).isEqualTo("READ");

        Permission defaultPerm = Permission.builder()
                .code("TEST_DEFAULT_MODULE")
                .name("Default Module Test")
                .build();
        assertThat(defaultPerm.getModule()).isEqualTo("SYSTEM");
    }

    @Test
    @DisplayName("RoleRepository: findWithPermissionsById nạp đầy đủ danh sách permissions")
    void roleRepository_findWithPermissionsById_loadsPermissionsEagerly() {
        Optional<Role> roleOpt = roleRepository.findWithPermissionsById(adminRole.getId());

        assertThat(roleOpt).isPresent();
        assertThat(roleOpt.get().getPermissions()).extracting(Permission::getCode)
                .containsExactlyInAnyOrder("TEST_USER_READ", "TEST_USER_CREATE");
    }

    @Test
    @DisplayName("RoleRepository: findAllByStatus lọc chính xác theo trạng thái ACTIVE/INACTIVE")
    void roleRepository_findAllByStatus_filtersCorrectly() {
        List<Role> activeRoles = roleRepository.findAllByStatus(RoleStatus.ACTIVE);
        assertThat(activeRoles).extracting(Role::getCode).contains("TEST_ADMIN");
        assertThat(activeRoles).extracting(Role::getCode).doesNotContain("TEST_CUSTOM");

        List<Role> inactiveRoles = roleRepository.findAllByStatus(RoleStatus.INACTIVE);
        assertThat(inactiveRoles).extracting(Role::getCode).contains("TEST_CUSTOM");
    }

    @Test
    @DisplayName("RoleRepository: findAllByIsSystemTrue lọc danh sách vai trò hệ thống")
    void roleRepository_findAllByIsSystemTrue_filtersCorrectly() {
        List<Role> systemRoles = roleRepository.findAllByIsSystemTrue();

        assertThat(systemRoles).extracting(Role::getCode).contains("TEST_ADMIN");
        assertThat(systemRoles).extracting(Role::getCode).doesNotContain("TEST_CUSTOM");
    }

    @Test
    @DisplayName("RoleRepository: isRoleAssignedToAnyUser và countUsersByRoleId phát hiện role đang được gán")
    void roleRepository_isRoleAssignedToAnyUser_worksAccurately() {
        assertThat(roleRepository.isRoleAssignedToAnyUser(adminRole.getId())).isFalse();
        assertThat(roleRepository.countUsersByRoleId(adminRole.getId())).isZero();

        User user = User.builder()
                .username("test_rbac_user")
                .email("test_rbac_user@dongly.vn")
                .passwordHash("hash")
                .fullName("Test RBAC User")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build();
        userRepository.save(user);

        assertThat(roleRepository.isRoleAssignedToAnyUser(adminRole.getId())).isTrue();
        assertThat(roleRepository.countUsersByRoleId(adminRole.getId())).isEqualTo(1L);
    }

    @Test
    @DisplayName("RoleRepository: existsByCodeIgnoreCaseAndIdNot kiểm tra trùng code khi cập nhật")
    void roleRepository_existsByCodeIgnoreCaseAndIdNot_works() {
        assertThat(roleRepository.existsByCodeIgnoreCaseAndIdNot("test_admin", UUID.randomUUID())).isTrue();
        assertThat(roleRepository.existsByCodeIgnoreCaseAndIdNot("test_admin", adminRole.getId())).isFalse();
    }

    @Test
    @DisplayName("PermissionRepository: tra cứu theo module và lấy danh sách distinct module")
    void permissionRepository_moduleQueries_workAccurately() {
        List<Permission> userPerms = permissionRepository.findAllByModule("USER");
        assertThat(userPerms).extracting(Permission::getCode).contains("TEST_USER_READ", "TEST_USER_CREATE");

        List<String> distinctModules = permissionRepository.findDistinctModules();
        assertThat(distinctModules).contains("USER");

        List<Permission> sortedPerms = permissionRepository.findAllByOrderByModuleAscCodeAsc();
        assertThat(sortedPerms).isNotEmpty();
    }
}
