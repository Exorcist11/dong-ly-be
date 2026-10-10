package com.dongly.modules.user.service;

import com.dongly.modules.user.dto.PermissionCatalogResponse;
import com.dongly.modules.user.dto.PermissionGroupResponse;
import com.dongly.modules.user.dto.PermissionResponse;
import com.dongly.modules.user.entity.Permission;
import com.dongly.modules.user.mapper.UserMapper;
import com.dongly.modules.user.repository.PermissionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Xử lý logic nghiệp vụ tra cứu danh mục quyền hạn RBAC.
 */
@Slf4j
@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    /**
     * Lấy toàn bộ danh mục quyền hạn hệ thống, có thể lọc theo module và cấu trúc gom nhóm.
     *
     * @param module tên module cần lọc (tùy chọn)
     * @return danh mục quyền hạn kèm danh sách phẳng và danh sách nhóm
     */
    @Transactional(readOnly = true)
    public PermissionCatalogResponse getPermissionCatalog(String module) {
        List<Permission> permissions = (module != null && !module.isBlank())
                ? permissionRepository.findAllByModuleOrderByCodeAsc(module.trim().toUpperCase())
                : permissionRepository.findAllByOrderByModuleAscCodeAsc();

        List<PermissionResponse> permissionResponses = permissions.stream()
                .map(UserMapper::toPermissionResponse)
                .toList();

        // Gom nhóm theo module và giữ nguyên thứ tự xuất hiện
        Map<String, List<PermissionResponse>> groupedByModule = permissionResponses.stream()
                .collect(Collectors.groupingBy(
                        PermissionResponse::module,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<PermissionGroupResponse> groups = groupedByModule.entrySet().stream()
                .map(entry -> new PermissionGroupResponse(entry.getKey(), entry.getValue()))
                .toList();

        return new PermissionCatalogResponse(
                permissionResponses.size(),
                groups,
                permissionResponses
        );
    }
}
