package com.dongly.modules.user.controller;

import com.dongly.common.api.ApiResponse;
import com.dongly.common.api.PageResponse;
import com.dongly.common.exception.AppException;
import com.dongly.common.exception.ErrorCode;
import com.dongly.modules.user.dto.CreateUserRequest;
import com.dongly.modules.user.dto.UpdateUserRequest;
import com.dongly.modules.user.dto.UpdateUserStatusRequest;
import com.dongly.modules.user.dto.UserResponse;
import com.dongly.modules.user.service.UserService;
import com.dongly.security.CurrentUser;
import com.dongly.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller quản lý tài khoản người dùng (User Management).
 */
@Tag(name = "User Management", description = "Các endpoint quản trị người dùng và phân quyền RBAC")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Lấy danh sách người dùng có phân trang (Yêu cầu quyền USER_READ)")
    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<PageResponse<UserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        if (size > 100) {
            size = 100; // Giới hạn an toàn theo docs/rules/api.md
        }

        Sort sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sort != null && sort.contains(",")) {
            String[] parts = sort.split(",");
            Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            sortObj = Sort.by(direction, parts[0]);
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), size, sortObj);
        Page<UserResponse> userPage = userService.getUsers(pageable);

        return ResponseEntity.ok(PageResponse.of(userPage, "Lấy danh sách người dùng thành công"));
    }

    @Operation(summary = "Lấy chi tiết người dùng theo ID (Yêu cầu quyền USER_READ hoặc chính chủ sở hữu)")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Yêu cầu xác thực tài khoản"));

        UserResponse user = userService.getUserById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", user));
    }

    @Operation(summary = "Tạo mới người dùng và gán vai trò (Yêu cầu quyền USER_CREATE)")
    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        UserResponse createdUser = userService.createUser(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo người dùng thành công", createdUser));
    }

    @Operation(summary = "Cập nhật thông tin người dùng (Yêu cầu quyền USER_UPDATE)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Yêu cầu xác thực tài khoản"));

        UserResponse updatedUser = userService.updateUser(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin người dùng thành công", updatedUser));
    }

    @Operation(summary = "Cập nhật trạng thái người dùng (Yêu cầu quyền USER_UPDATE)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Yêu cầu xác thực tài khoản"));

        UserResponse updatedUser = userService.updateUserStatus(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái người dùng thành công", updatedUser));
    }
}
