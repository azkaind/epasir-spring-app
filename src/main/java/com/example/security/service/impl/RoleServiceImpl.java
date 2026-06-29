package com.example.security.service.impl;

import com.example.security.dto.request.CreateRoleRequest;
import com.example.security.dto.response.PermissionResponse;
import com.example.security.dto.response.RoleResponse;
import com.example.security.entity.AppRole;
import com.example.security.entity.Permission;
import com.example.security.exception.BadRequestException;
import com.example.security.exception.UserAlreadyExistsException;
import com.example.security.exception.UserNotFoundException;
import com.example.security.repository.AppRoleRepository;
import com.example.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RoleServiceImpl {

    private final AppRoleRepository roleRepository;
    private final UserRepository    userRepository;

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAllWithUserCount().stream()
                .map(row -> {
                    AppRole role = (AppRole) row[0];
                    long count   = (Long)    row[1];
                    return RoleResponse.from(role, count);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleResponse getRoleById(UUID id) {
        AppRole role = findRoleOrThrow(id);
        return RoleResponse.from(role, userRepository.countByRole(role));
    }

    public RoleResponse createRole(CreateRoleRequest req) {
        if (roleRepository.existsByName(req.getName())) {
            throw new UserAlreadyExistsException("Role '" + req.getName() + "' sudah ada.");
        }
        AppRole role = AppRole.builder()
                .name(req.getName())
                .description(req.getDescription())
                .color(req.getColor() != null ? req.getColor() : "#1A237E")
                .systemRole(false)
                .permissions(req.getPermissions() != null
                        ? new HashSet<>(req.getPermissions()) : new HashSet<>())
                .build();
        role = roleRepository.save(role);
        log.info("Role '{}' berhasil dibuat", role.getName());
        return RoleResponse.from(role, 0);
    }

    public RoleResponse updateRole(UUID id, Map<String, Object> updates) {
        AppRole role = findRoleOrThrow(id);
        if (updates.containsKey("description")) role.setDescription((String) updates.get("description"));
        if (updates.containsKey("color"))       role.setColor((String) updates.get("color"));
        if (updates.containsKey("permissions")) {
            @SuppressWarnings("unchecked")
            List<String> permNames = (List<String>) updates.get("permissions");
            Set<Permission> perms = permNames.stream()
                    .map(Permission::valueOf).collect(Collectors.toSet());
            role.setPermissions(perms);
        }
        role = roleRepository.save(role);
        long count = userRepository.countByRole(role);
        log.info("Role '{}' berhasil diperbarui", role.getName());
        return RoleResponse.from(role, count);
    }

    public void deleteRole(UUID id) {
        AppRole role = findRoleOrThrow(id);
        if (role.isSystemRole()) {
            throw new BadRequestException("Role sistem '" + role.getName() + "' tidak dapat dihapus.");
        }
        long userCount = userRepository.countByRole(role);
        if (userCount > 0) {
            throw new BadRequestException("Role '" + role.getName() + "' masih digunakan oleh " +
                    userCount + " pengguna. Pindahkan pengguna ke role lain terlebih dahulu.");
        }
        roleRepository.delete(role);
        log.info("Role '{}' berhasil dihapus", role.getName());
    }

    public RoleResponse assignPermissions(UUID roleId, Set<Permission> permissions) {
        AppRole role = findRoleOrThrow(roleId);
        role.setPermissions(new HashSet<>(permissions));
        role = roleRepository.save(role);
        log.info("Permission role '{}' diperbarui: {} permission", role.getName(), permissions.size());
        return RoleResponse.from(role, userRepository.countByRole(role));
    }

    public RoleResponse addPermission(UUID roleId, Permission permission) {
        AppRole role = findRoleOrThrow(roleId);
        role.addPermission(permission);
        role = roleRepository.save(role);
        return RoleResponse.from(role, userRepository.countByRole(role));
    }

    public RoleResponse removePermission(UUID roleId, Permission permission) {
        AppRole role = findRoleOrThrow(roleId);
        role.removePermission(permission);
        role = roleRepository.save(role);
        return RoleResponse.from(role, userRepository.countByRole(role));
    }

    @Transactional(readOnly = true)
    public Map<String, List<PermissionResponse>> getAllPermissionsGrouped() {
        return Arrays.stream(Permission.values())
                .map(PermissionResponse::from)
                .collect(Collectors.groupingBy(PermissionResponse::getCategory));
    }

    private AppRole findRoleOrThrow(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Role tidak ditemukan: " + id));
    }
}
