package com.dongly.modules.user.service;

import com.dongly.common.exception.AppException;
import com.dongly.common.exception.ErrorCode;
import com.dongly.common.exception.ResourceNotFoundException;
import com.dongly.modules.user.dto.AssignRolePermissionsRequest;
import com.dongly.modules.user.dto.CreateRoleRequest;
import com.dongly.modules.user.dto.PermissionResponse;
import com.dongly.modules.user.dto.RoleDetailResponse;
import com.dongly.modules.user.dto.RoleResponse;
import com.dongly.modules.user.dto.UpdateRoleRequest;
import com.dongly.modules.user.dto.UpdateRoleStatusRequest;
import com.dongly.modules.user.entity.Permission;
import com.dongly.modules.user.entity.Role;
import com.dongly.modules.user.entity.RoleStatus;
import com.dongly.modules.user.mapper.UserMapper;
import com.dongly.modules.user.repository.PermissionRepository;
import com.dongly.modules.user.repository.RoleRepository;
import com.dongly.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Xử lý toàn bộ logic nghiệp vụ quản lý Vai trò (Role) và Phân quyền Vai trò (Role-Permission).
 */
@Slf4j
@Service
public class RoleService {

    private static final Set<String> CORE_ADMIN_PERMISSIONS = Set.of(
            "ROLE_READ", "ROLE_CREATE", "ROLE_UPDATE", "ROLE_ASSIGN", "PERMISSION_READ"
    );

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    /**
     * Lấy danh sách vai trò có phân trang, tìm kiếm theo code/tên và lọc theo trạng thái.
     */
    @Transactional(readOnly = true)
    public Page<RoleResponse> getRoles(String search, RoleStatus status, Pageable pageable) {
        Specification<Role> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("code")), pattern)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return roleRepository.findAll(spec, pageable).map(UserMapper::toRoleResponse);
    }

    /**
     * Lấy thông tin chi tiết một vai trò kèm đầy đủ danh sách quyền hạn.
     */
    @Transactional(readOnly = true)
    public RoleDetailResponse getRoleById(UUID id) {
        Role role = roleRepository.findWithPermissionsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vai trò", id));

        return UserMapper.toRoleDetailResponse(role);
    }

    /**
     * Tạo mới vai trò tùy chỉnh.
     */
    @Transactional
    public RoleDetailResponse createRole(CreateRoleRequest request, CurrentUser creator) {
        String code = request.code().trim().toUpperCase();

        if (roleRepository.existsByCode(code)) {
            throw new AppException(
                    ErrorCode.RESOURCE_ALREADY_EXISTS,
                    "Mã vai trò '" + code + "' đã tồn tại trong hệ thống"
            );
        }

        // Ngăn chặn tự nâng quyền: Người dùng không phải ADMIN không được cấp quyền mà mình không sở hữu
        if (creator != null && !creator.isAdmin() && request.permissionCodes() != null) {
            boolean hasUnauthorizedPerm = request.permissionCodes().stream()
                    .anyMatch(p -> !creator.hasPermission(p));
            if (hasUnauthorizedPerm) {
                throw new AppException(
                        ErrorCode.ACCESS_DENIED,
                        "Bạn không thể cấp các quyền mà bạn không sở hữu khi tạo vai trò mới (ngăn chặn tự nâng quyền)"
                );
            }
        }

        Set<Permission> permissions = new HashSet<>();
        if (request.permissionCodes() != null && !request.permissionCodes().isEmpty()) {
            List<Permission> foundPerms = permissionRepository.findAllByCodeIn(request.permissionCodes());
            if (foundPerms.size() != request.permissionCodes().size()) {
                Set<String> foundCodes = foundPerms.stream().map(Permission::getCode).collect(Collectors.toSet());
                Set<String> missing = request.permissionCodes().stream()
                        .filter(c -> !foundCodes.contains(c))
                        .collect(Collectors.toSet());
                throw new AppException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy các quyền trong hệ thống: " + missing
                );
            }
            permissions.addAll(foundPerms);
        }

        Role role = Role.builder()
                .code(code)
                .name(request.name().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .status(RoleStatus.ACTIVE)
                .isSystem(false)
                .permissions(permissions)
                .build();

        Role savedRole = roleRepository.save(role);
        log.info("Tạo mới vai trò thành công: id={}, code={}, creator={}",
                savedRole.getId(), savedRole.getCode(), creator != null ? creator.username() : "SYSTEM");

        return UserMapper.toRoleDetailResponse(savedRole);
    }

    /**
     * Cập nhật thông tin vai trò (Tên và Mô tả). Mã vai trò (code) bất biến.
     */
    @Transactional
    public RoleResponse updateRole(UUID id, UpdateRoleRequest request, CurrentUser updater) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vai trò", id));

        role.setName(request.name().trim());
        role.setDescription(request.description() != null ? request.description().trim() : null);

        Role saved = roleRepository.save(role);
        log.info("Cập nhật thông tin vai trò thành công: id={}, updater={}",
                id, updater != null ? updater.username() : "SYSTEM");

        return UserMapper.toRoleResponse(saved);
    }

    /**
     * Cập nhật trạng thái hoạt động của vai trò (ACTIVE / INACTIVE).
     * Cấm vô hiệu hóa vai trò hệ thống và vai trò đang có người dùng gán.
     */
    @Transactional
    public RoleResponse updateRoleStatus(UUID id, UpdateRoleStatusRequest request, CurrentUser updater) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vai trò", id));

        if ((role.isSystem() || "ADMIN".equalsIgnoreCase(role.getCode())) && request.status() == RoleStatus.INACTIVE) {
            throw new AppException(
                    ErrorCode.SYSTEM_ROLE_PROTECTED,
                    "Không được phép vô hiệu hóa vai trò hệ thống '" + role.getCode() + "'"
            );
        }

        if (request.status() == RoleStatus.INACTIVE && roleRepository.isRoleAssignedToAnyUser(id)) {
            long userCount = roleRepository.countUsersByRoleId(id);
            throw new AppException(
                    ErrorCode.ROLE_IN_USE,
                    "Không thể vô hiệu hóa vai trò đang được gán cho " + userCount + " người dùng"
            );
        }

        role.setStatus(request.status());
        Role saved = roleRepository.save(role);
        log.info("Cập nhật trạng thái vai trò thành công: id={}, status={}, updater={}",
                id, request.status(), updater != null ? updater.username() : "SYSTEM");

        return UserMapper.toRoleResponse(saved);
    }

    /**
     * Xóa vai trò tùy chỉnh. Cấm xóa vai trò hệ thống hoặc vai trò đang được gán cho người dùng.
     */
    @Transactional
    public void deleteRole(UUID id, CurrentUser deleter) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vai trò", id));

        if (role.isSystem() || "ADMIN".equalsIgnoreCase(role.getCode())) {
            throw new AppException(
                    ErrorCode.SYSTEM_ROLE_PROTECTED,
                    "Không được phép xóa vai trò hệ thống '" + role.getCode() + "'"
            );
        }

        if (roleRepository.isRoleAssignedToAnyUser(id)) {
            long userCount = roleRepository.countUsersByRoleId(id);
            throw new AppException(
                    ErrorCode.ROLE_IN_USE,
                    "Không thể xóa vai trò đang được gán cho " + userCount + " người dùng"
            );
        }

        roleRepository.delete(role);
        log.info("Xóa vai trò thành công: id={}, code={}, deleter={}",
                id, role.getCode(), deleter != null ? deleter.username() : "SYSTEM");
    }

    /**
     * Xem danh sách quyền hạn được gán cho một vai trò cụ thể.
     */
    @Transactional(readOnly = true)
    public List<PermissionResponse> getRolePermissions(UUID id) {
        Role role = roleRepository.findWithPermissionsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vai trò", id));

        return role.getPermissions().stream()
                .filter(Objects::nonNull)
                .map(UserMapper::toPermissionResponse)
                .sorted(Comparator.comparing(PermissionResponse::code))
                .toList();
    }

    /**
     * Gán / Cập nhật toàn bộ danh mục quyền hạn của vai trò trong một Transaction đồng bộ.
     * Bảo vệ vai trò ADMIN không bị thu hồi các quyền quản trị cốt lõi.
     * Ngăn chặn người dùng tự nâng quyền.
     */
    @Transactional
    public RoleDetailResponse assignRolePermissions(
            UUID id,
            AssignRolePermissionsRequest request,
            CurrentUser updater
    ) {
        Role role = roleRepository.findWithPermissionsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vai trò", id));

        // Ngăn chặn thu hồi các quyền quản trị thiết yếu của vai trò ADMIN hệ thống
        if (role.isSystem() || "ADMIN".equalsIgnoreCase(role.getCode())) {
            Set<String> requestedCodes = request.permissionCodes();
            boolean retainsCore = CORE_ADMIN_PERMISSIONS.stream().allMatch(requestedCodes::contains);
            if (!retainsCore) {
                Set<String> missingCore = CORE_ADMIN_PERMISSIONS.stream()
                        .filter(core -> !requestedCodes.contains(core))
                        .collect(Collectors.toSet());
                throw new AppException(
                        ErrorCode.BUSINESS_RULE_VIOLATION,
                        "Không được phép thu hồi các quyền quản trị cốt lõi khỏi vai trò ADMIN hệ thống: " + missingCore
                );
            }
        }

        // Ngăn chặn tự nâng quyền: Người dùng không có quyền quản trị tối cao không được cấp quyền mà mình không sở hữu
        if (updater != null && !updater.isAdmin()) {
            boolean hasUnauthorizedPerm = request.permissionCodes().stream()
                    .anyMatch(p -> !updater.hasPermission(p));
            if (hasUnauthorizedPerm) {
                throw new AppException(
                        ErrorCode.ACCESS_DENIED,
                        "Bạn không thể phân bổ các quyền mà bạn không sở hữu (ngăn chặn tự nâng quyền)"
                );
            }
        }

        List<Permission> newPermissions = new ArrayList<>();
        if (!request.permissionCodes().isEmpty()) {
            newPermissions = permissionRepository.findAllByCodeIn(request.permissionCodes());
            if (newPermissions.size() != request.permissionCodes().size()) {
                Set<String> foundCodes = newPermissions.stream().map(Permission::getCode).collect(Collectors.toSet());
                Set<String> missing = request.permissionCodes().stream()
                        .filter(c -> !foundCodes.contains(c))
                        .collect(Collectors.toSet());
                throw new AppException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy các quyền trong danh mục hệ thống: " + missing
                );
            }
        }

        // Cập nhật danh sách quyền atomically trong transaction
        if (role.getPermissions() == null) {
            role.setPermissions(new HashSet<>(newPermissions));
        } else {
            try {
                role.getPermissions().clear();
                role.getPermissions().addAll(newPermissions);
            } catch (UnsupportedOperationException e) {
                role.setPermissions(new HashSet<>(newPermissions));
            }
        }

        Role saved = roleRepository.save(role);
        log.info("Cập nhật danh sách quyền cho vai trò thành công: id={}, count={}, updater={}",
                id, newPermissions.size(), updater != null ? updater.username() : "SYSTEM");

        return UserMapper.toRoleDetailResponse(saved);
    }
}
