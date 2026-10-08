package com.dongly.modules.user.service;

import com.dongly.common.exception.AppException;
import com.dongly.common.exception.ErrorCode;
import com.dongly.common.exception.ResourceNotFoundException;
import com.dongly.modules.auth.repository.RefreshTokenRepository;
import com.dongly.modules.user.dto.CreateUserRequest;
import com.dongly.modules.user.dto.UpdateUserRequest;
import com.dongly.modules.user.dto.UpdateUserStatusRequest;
import com.dongly.modules.user.dto.UserResponse;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Xử lý logic nghiệp vụ quản lý người dùng (User Management).
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
            List<Role> foundRoles = roleRepository.findAllByCodeIn(request.roleCodes());
            roles.addAll(foundRoles);
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
            List<Role> roles = roleRepository.findAllByCodeIn(request.roleCodes());
            user.setRoles(new HashSet<>(roles));
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
}
