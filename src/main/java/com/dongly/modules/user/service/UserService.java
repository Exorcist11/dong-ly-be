package com.dongly.modules.user.service;

import com.dongly.common.exception.AppException;
import com.dongly.common.exception.ErrorCode;
import com.dongly.common.exception.ResourceNotFoundException;
import com.dongly.modules.auth.repository.RefreshTokenRepository;
import com.dongly.modules.user.dto.CreateUserRequest;
import com.dongly.modules.user.dto.RoleResponse;
import com.dongly.modules.user.dto.UpdateUserRequest;
import com.dongly.modules.user.dto.UpdateUserRolesRequest;
import com.dongly.modules.user.dto.UpdateUserStatusRequest;
import com.dongly.modules.user.dto.UserResponse;
import com.dongly.modules.user.entity.Permission;
import com.dongly.modules.user.entity.Role;
import com.dongly.modules.user.entity.User;
import com.dongly.modules.user.entity.UserStatus;
import com.dongly.modules.user.mapper.UserMapper;
import com.dongly.modules.user.repository.RoleRepository;
import com.dongly.modules.user.repository.UserRepository;
import com.dongly.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Xử lý logic nghiệp vụ quản lý người dùng và phân bổ vai trò (User Management & User-Role RBAC).
 */
@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Tạo mới người dùng, mã hóa mật khẩu và gán vai trò tương ứng.
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request, CurrentUser creator) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new AppException(
                    ErrorCode.RESOURCE_ALREADY_EXISTS,
                    "Tên đăng nhập '" + username + "' đã tồn tại trong hệ thống"
            );
        }

        if (userRepository.existsByEmail(email)) {
            throw new AppException(
                    ErrorCode.RESOURCE_ALREADY_EXISTS,
                    "Email '" + email + "' đã tồn tại trong hệ thống"
            );
        }

        Set<Role> roles = new HashSet<>();
        if (request.roleCodes() != null && !request.roleCodes().isEmpty()) {
            Set<Role> validatedRoles = resolveAndValidateRoles(request.roleCodes(), null, creator);
            roles.addAll(validatedRoles);
        } else {
            // Mặc định gán vai trò CUSTOMER nếu không truyền vai trò
            roleRepository.findByCode("CUSTOMER").ifPresent(roles::add);
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .phone(request.phone() != null ? request.phone().trim() : null)
                .status(UserStatus.ACTIVE)
                .roles(roles)
                .createdBy(creator != null ? creator.username() : "SYSTEM")
                .build();

        User savedUser = userRepository.save(user);
        log.info("Tạo mới người dùng thành công: id={}, username={}", savedUser.getId(), savedUser.getUsername());

        return UserMapper.toResponse(savedUser);
    }

    /**
     * Lấy danh sách người dùng có phân trang.
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserMapper::toResponse);
    }

    /**
     * Lấy thông tin người dùng theo ID kèm kiểm tra chống lỗ hổng IDOR.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id, CurrentUser currentUser) {
        // Chống IDOR: Chỉ cho phép truy cập nếu có quyền USER_READ hoặc là chính chủ sở hữu
        if (!currentUser.hasPermission("USER_READ") && !currentUser.isAdmin() && !currentUser.id().equals(id)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Bạn không có quyền xem thông tin người dùng này");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", id));

        return UserMapper.toResponse(user);
    }

    /**
     * Cập nhật thông tin người dùng (Họ tên, email, SĐT, danh sách vai trò).
     */
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request, CurrentUser currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", id));

        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new AppException(
                    ErrorCode.RESOURCE_ALREADY_EXISTS,
                    "Email '" + email + "' đã tồn tại ở tài khoản khác"
            );
        }

        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(request.phone() != null ? request.phone().trim() : null);
        user.setUpdatedBy(currentUser != null ? currentUser.username() : "SYSTEM");

        if (request.roleCodes() != null) {
            Set<Role> validatedRoles = resolveAndValidateRoles(request.roleCodes(), user, currentUser);
            user.setRoles(validatedRoles);
        }

        User updatedUser = userRepository.save(user);
        log.info("Cập nhật thông tin người dùng thành công: id={}", updatedUser.getId());

        return UserMapper.toResponse(updatedUser);
    }

    /**
     * Cập nhật trạng thái người dùng (ACTIVE, INACTIVE, LOCKED). Không hard delete.
     */
    @Transactional
    public UserResponse updateUserStatus(UUID id, UpdateUserStatusRequest request, CurrentUser currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", id));

        // Không cho phép tự khóa tài khoản của chính mình nếu là admin đang thực thi
        if (currentUser != null && currentUser.id().equals(id) && request.status() != UserStatus.ACTIVE) {
            throw new AppException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Không thể tự vô hiệu hóa hoặc khóa tài khoản của chính mình"
            );
        }

        user.setStatus(request.status());
        user.setUpdatedBy(currentUser != null ? currentUser.username() : "SYSTEM");

        // Nếu chuyển sang không hoạt động hoặc bị khóa, thu hồi toàn bộ Refresh Token của user đó
        if (request.status() != UserStatus.ACTIVE) {
            refreshTokenRepository.revokeAllActiveTokensByUserId(id);
        }

        User updatedUser = userRepository.save(user);
        log.info("Cập nhật trạng thái người dùng thành công: id={}, status={}", id, request.status());

        return UserMapper.toResponse(updatedUser);
    }

    /**
     * Lấy danh sách vai trò đang được gán cho một người dùng cụ thể.
     */
    @Transactional(readOnly = true)
    public List<RoleResponse> getUserRoles(UUID id, CurrentUser currentUser) {
        // Kiểm tra quyền hạn: USER_READ, ROLE_READ, ADMIN hoặc chính chủ tài khoản
        if (!currentUser.hasPermission("USER_READ")
                && !currentUser.hasPermission("ROLE_READ")
                && !currentUser.isAdmin()
                && !currentUser.id().equals(id)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Bạn không có quyền xem vai trò của người dùng này");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", id));

        if (user.getRoles() == null) {
            return Collections.emptyList();
        }

        return user.getRoles().stream()
                .filter(Objects::nonNull)
                .map(UserMapper::toRoleResponse)
                .sorted(Comparator.comparing(RoleResponse::code))
                .toList();
    }

    /**
     * Cập nhật danh sách vai trò của người dùng trong một Transaction đồng bộ.
     */
    @Transactional
    public List<RoleResponse> updateUserRoles(UUID id, UpdateUserRolesRequest request, CurrentUser currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", id));

        Set<Role> validatedRoles = resolveAndValidateRoles(request.roleCodes(), user, currentUser);

        user.setRoles(validatedRoles);
        user.setUpdatedBy(currentUser != null ? currentUser.username() : "SYSTEM");

        User savedUser = userRepository.save(user);
        log.info("Cập nhật vai trò cho người dùng thành công: userId={}, rolesCount={}, updater={}",
                id, validatedRoles.size(), currentUser != null ? currentUser.username() : "SYSTEM");

        return savedUser.getRoles().stream()
                .filter(Objects::nonNull)
                .map(UserMapper::toRoleResponse)
                .sorted(Comparator.comparing(RoleResponse::code))
                .toList();
    }

    /**
     * Phương thức dùng chung xác thực và phân giải danh sách vai trò:
     * 1. Kiểm tra thẩm quyền phân bổ vai trò của người thực thi (ROLE_ASSIGN hoặc ADMIN).
     * 2. Xác thực tất cả vai trò phải tồn tại trong DB (ném RESOURCE_NOT_FOUND nếu thiếu).
     * 3. Xác thực trạng thái của vai trò phải là ACTIVE (ném BUSINESS_RULE_VIOLATION nếu vai trò bị vô hiệu hóa).
     * 4. Ngăn chặn tự nâng quyền (Privilege Escalation Prevention):
     *    - Người không phải ADMIN không được phân bổ vai trò ADMIN.
     *    - Người thực thi không được gán vai trò chứa quyền hạn mà bản thân không sở hữu.
     *    - Người dùng không được tự thay đổi vai trò của chính mình.
     * 5. Ràng buộc bảo vệ vai trò hệ thống:
     *    - Không cho phép thu hồi vai trò ADMIN của tài khoản quản trị viên cuối cùng trong hệ thống.
     */
    private Set<Role> resolveAndValidateRoles(Set<String> roleCodes, User targetUser, CurrentUser currentUser) {
        if (roleCodes == null) {
            return Collections.emptySet();
        }

        // 1. Kiểm tra thẩm quyền gán vai trò
        if (currentUser != null && !currentUser.isAdmin() && !currentUser.hasPermission("ROLE_ASSIGN")) {
            throw new AppException(
                    ErrorCode.ACCESS_DENIED,
                    "Bạn không có quyền phân bổ vai trò cho người dùng (yêu cầu quyền ROLE_ASSIGN)"
            );
        }

        // 2. Tra cứu và xác thực sự tồn tại của tất cả các vai trò
        List<Role> foundRoles = Collections.emptyList();
        if (!roleCodes.isEmpty()) {
            foundRoles = roleRepository.findAllByCodeIn(roleCodes);
            if (foundRoles.size() != roleCodes.size()) {
                Set<String> foundCodes = foundRoles.stream().map(Role::getCode).collect(Collectors.toSet());
                Set<String> missing = roleCodes.stream()
                        .filter(c -> !foundCodes.contains(c))
                        .collect(Collectors.toSet());
                throw new AppException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy các vai trò trong hệ thống: " + missing
                );
            }
        }

        // 3. Kiểm tra trạng thái hoạt động: Không cho phép gán vai trò đang bị INACTIVE
        List<String> inactiveRoles = foundRoles.stream()
                .filter(r -> !r.isActive())
                .map(Role::getCode)
                .toList();
        if (!inactiveRoles.isEmpty()) {
            throw new AppException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Không thể gán vai trò đang bị vô hiệu hóa: " + inactiveRoles
            );
        }

        // 4. Ngăn chặn tự nâng quyền (Privilege Escalation Prevention)
        if (currentUser != null && !currentUser.isAdmin()) {
            // Không được phân bổ vai trò ADMIN nếu bản thân không phải ADMIN
            boolean containsAdminRole = roleCodes.stream().anyMatch("ADMIN"::equalsIgnoreCase);
            if (containsAdminRole) {
                throw new AppException(
                        ErrorCode.ACCESS_DENIED,
                        "Chỉ quản trị viên hệ thống mới có quyền phân bổ vai trò ADMIN"
                );
            }

            // Không được gán vai trò chứa quyền hạn mà bản thân người thực thi không sở hữu
            Set<String> currentUserPermissions = currentUser.permissions();
            for (Role role : foundRoles) {
                if (role.getPermissions() != null) {
                    boolean hasUnpossessedPerm = role.getPermissions().stream()
                            .map(Permission::getCode)
                            .anyMatch(permCode -> !currentUserPermissions.contains(permCode));
                    if (hasUnpossessedPerm) {
                        throw new AppException(
                                ErrorCode.ACCESS_DENIED,
                                "Không thể phân bổ vai trò '" + role.getCode() + "' vì vai trò này chứa quyền hạn mà bạn không sở hữu (ngăn chặn tự nâng quyền)"
                        );
                    }
                }
            }

            // Không được tự thay đổi vai trò của chính mình để tránh leo thang quyền
            if (targetUser != null && targetUser.getId().equals(currentUser.id())) {
                throw new AppException(
                        ErrorCode.ACCESS_DENIED,
                        "Không được phép tự thay đổi hoặc nâng vai trò của chính mình"
                );
            }
        }

        // 5. Ràng buộc bảo vệ tài khoản quản trị cuối cùng
        if (targetUser != null && targetUser.getRoles() != null) {
            boolean hadAdminRole = targetUser.getRoles().stream()
                    .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()));
            boolean willHaveAdminRole = roleCodes.stream().anyMatch("ADMIN"::equalsIgnoreCase);

            if (hadAdminRole && !willHaveAdminRole) {
                Role adminRole = roleRepository.findByCode("ADMIN").orElse(null);
                if (adminRole != null) {
                    long activeAdminCount = roleRepository.countUsersByRoleId(adminRole.getId());
                    if (activeAdminCount <= 1) {
                        throw new AppException(
                                ErrorCode.BUSINESS_RULE_VIOLATION,
                                "Không thể thu hồi vai trò ADMIN của tài khoản quản trị viên cuối cùng trong hệ thống"
                        );
                    }
                }
            }
        }

        return new HashSet<>(foundRoles);
    }
}
