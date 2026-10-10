package com.dongly.modules.user.controller;

import com.dongly.common.api.ApiResponse;
import com.dongly.modules.user.dto.PermissionCatalogResponse;
import com.dongly.modules.user.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cung cấp danh mục quyền hạn của toàn hệ thống (Permission Catalog).
 */
@Tag(name = "Permissions", description = "Các endpoint tra cứu danh mục quyền hạn RBAC")
@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Operation(summary = "Lấy danh mục quyền hạn hệ thống theo cấu trúc nhóm (Yêu cầu quyền PERMISSION_READ hoặc ROLE_READ)")
    @GetMapping
    @PreAuthorize("hasAuthority('PERMISSION_READ') or hasAuthority('ROLE_READ')")
    public ResponseEntity<ApiResponse<PermissionCatalogResponse>> getPermissions(
            @RequestParam(required = false) String module
    ) {
        PermissionCatalogResponse catalog = permissionService.getPermissionCatalog(module);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục quyền hạn thành công", catalog));
    }
}
