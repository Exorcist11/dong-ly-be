package com.dongly.modules.user.controller;

import com.dongly.common.api.ApiResponse;
import com.dongly.common.api.PageResponse;
import com.dongly.modules.user.dto.AssignRolePermissionsRequest;
import com.dongly.modules.user.dto.CreateRoleRequest;
import com.dongly.modules.user.dto.PermissionResponse;
import com.dongly.modules.user.dto.RoleDetailResponse;
import com.dongly.modules.user.dto.RoleResponse;
import com.dongly.modules.user.dto.UpdateRoleRequest;
import com.dongly.modules.user.dto.UpdateRoleStatusRequest;
import com.dongly.modules.user.entity.RoleStatus;
import com.dongly.modules.user.service.RoleService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller quản lý vai trò và phân quyền (Role Management & Role-Permission).
 */
@Tag(name = "Roles", description = "Các endpoint quản trị vai trò và phân bổ quyền hạn RBAC")
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "Lấy danh sách vai trò có phân trang, tìm kiếm và lọc trạng thái (Yêu cầu quyền ROLE_READ)")
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<PageResponse<RoleResponse>> getRoles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RoleStatus status
    ) {
        if (size > 100) {
            size = 100;
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
        Page<RoleResponse> rolePage = roleService.getRoles(search, status, pageable);

        return ResponseEntity.ok(PageResponse.of(rolePage, "Lấy danh sách vai trò thành công"));
    }

    @Operation(summary = "Lấy chi tiết vai trò kèm toàn bộ quyền hạn (Yêu cầu quyền ROLE_READ)")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<ApiResponse<RoleDetailResponse>> getRoleById(@PathVariable UUID id) {
        RoleDetailResponse role = roleService.getRoleById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chi tiết vai trò thành công", role));
    }

    @Operation(summary = "Tạo mới vai trò tùy chỉnh (Yêu cầu quyền ROLE_CREATE)")
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public ResponseEntity<ApiResponse<RoleDetailResponse>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        RoleDetailResponse createdRole = roleService.createRole(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo vai trò thành công", createdRole));
    }

    @Operation(summary = "Cập nhật tên và mô tả vai trò (Yêu cầu quyền ROLE_UPDATE)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        RoleResponse updatedRole = roleService.updateRole(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật vai trò thành công", updatedRole));
    }

    @Operation(summary = "Cập nhật trạng thái hoạt động vai trò (Yêu cầu quyền ROLE_UPDATE)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRoleStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleStatusRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        RoleResponse updatedRole = roleService.updateRoleStatus(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái vai trò thành công", updatedRole));
    }

    @Operation(summary = "Xem danh sách quyền hạn được gán cho vai trò (Yêu cầu quyền ROLE_READ)")
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getRolePermissions(@PathVariable UUID id) {
        List<PermissionResponse> permissions = roleService.getRolePermissions(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách quyền của vai trò thành công", permissions));
    }

    @Operation(summary = "Gán/Cập nhật toàn bộ danh mục quyền cho vai trò (Yêu cầu quyền ROLE_ASSIGN)")
    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_ASSIGN')")
    public ResponseEntity<ApiResponse<RoleDetailResponse>> assignRolePermissions(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRolePermissionsRequest request
    ) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        RoleDetailResponse updatedRole = roleService.assignRolePermissions(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật quyền hạn cho vai trò thành công", updatedRole));
    }

    @Operation(summary = "Xóa vai trò tùy chỉnh (Yêu cầu quyền ROLE_DELETE)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable UUID id) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser().orElse(null);
        roleService.deleteRole(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Xóa vai trò thành công", null));
    }
}
