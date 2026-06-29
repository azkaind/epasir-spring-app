package com.example.security.controller;

import com.example.security.dto.request.CreateRoleRequest;
import com.example.security.dto.response.ApiResponse;
import com.example.security.dto.response.PermissionResponse;
import com.example.security.dto.response.RoleResponse;
import com.example.security.entity.Permission;
import com.example.security.service.impl.RoleServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleServiceImpl roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('role:view')")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.success(roleService.getAllRoles(), "Data role berhasil dimuat"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role:view')")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(roleService.getRoleById(id), "Role berhasil dimuat"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role:create')")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(roleService.createRole(request), "Role berhasil dibuat"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('role:edit')")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable UUID id, @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(ApiResponse.success(roleService.updateRole(id, updates), "Role berhasil diperbarui"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable UUID id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success("Role berhasil dihapus"));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:assign_permission')")
    public ResponseEntity<ApiResponse<RoleResponse>> assignPermissions(
            @PathVariable UUID id, @RequestBody Map<String, Set<Permission>> body) {
        return ResponseEntity.ok(ApiResponse.success(
                roleService.assignPermissions(id, body.get("permissions")), "Permission berhasil ditetapkan"));
    }

    @PostMapping("/{id}/permissions/{permission}")
    @PreAuthorize("hasAuthority('role:assign_permission')")
    public ResponseEntity<ApiResponse<RoleResponse>> addPermission(
            @PathVariable UUID id, @PathVariable Permission permission) {
        return ResponseEntity.ok(ApiResponse.success(
                roleService.addPermission(id, permission),
                "Permission '" + permission.getAuthority() + "' ditambahkan"));
    }

    @DeleteMapping("/{id}/permissions/{permission}")
    @PreAuthorize("hasAuthority('role:assign_permission')")
    public ResponseEntity<ApiResponse<RoleResponse>> removePermission(
            @PathVariable UUID id, @PathVariable Permission permission) {
        return ResponseEntity.ok(ApiResponse.success(
                roleService.removePermission(id, permission),
                "Permission '" + permission.getAuthority() + "' dihapus"));
    }

    @GetMapping("/permissions/all")
    @PreAuthorize("hasAuthority('role:view')")
    public ResponseEntity<ApiResponse<Map<String, List<PermissionResponse>>>> getAllPermissions() {
        return ResponseEntity.ok(ApiResponse.success(roleService.getAllPermissionsGrouped(), "Permission berhasil dimuat"));
    }
}
