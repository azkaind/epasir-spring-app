package com.example.security.controller;

import com.example.security.dto.request.CreateUserRequest;
import com.example.security.dto.request.UpdateUserRequest;
import com.example.security.dto.response.ApiResponse;
import com.example.security.dto.response.UserDetailResponse;
import com.example.security.dto.response.UserStatsResponse;
import com.example.security.service.impl.UserManagementServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementServiceImpl userService;

    @GetMapping
    @PreAuthorize("hasAuthority('user:view')")
    public ResponseEntity<ApiResponse<Page<UserDetailResponse>>> getUsers(
            @RequestParam(required = false) String  search,
            @RequestParam(required = false) UUID    roleId,
            @RequestParam(required = false) Boolean enabled,
            @PageableDefault(size = 10, sort = "username") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.getUsers(search, roleId, enabled, pageable), "Data pengguna berhasil dimuat"));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('user:view')")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getUserStats() {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserStats(), "Statistik berhasil dimuat"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:view')")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id), "Data pengguna berhasil dimuat"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    public ResponseEntity<ApiResponse<UserDetailResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal UserDetails admin) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userService.createUser(request, admin.getUsername()), "Pengguna berhasil dibuat"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('user:edit')")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateUser(
            @PathVariable UUID id,
            @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserDetails admin) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateUser(id, request, admin.getUsername()), "Pengguna berhasil diperbarui"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable UUID id, @AuthenticationPrincipal UserDetails admin) {
        userService.deleteUser(id, admin.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Pengguna berhasil dihapus"));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasAuthority('user:assign_role')")
    public ResponseEntity<ApiResponse<UserDetailResponse>> assignRole(
            @PathVariable UUID id,
            @RequestBody Map<String, UUID> body,
            @AuthenticationPrincipal UserDetails admin) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.assignRole(id, body.get("roleId"), admin.getUsername()), "Role berhasil ditetapkan"));
    }

    @PatchMapping("/{id}/toggle-lock")
    @PreAuthorize("hasAuthority('user:lock')")
    public ResponseEntity<ApiResponse<UserDetailResponse>> toggleLock(
            @PathVariable UUID id, @AuthenticationPrincipal UserDetails admin) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.toggleLock(id, admin.getUsername()), "Status kunci berhasil diperbarui"));
    }

    @PatchMapping("/{id}/toggle-enable")
    @PreAuthorize("hasAuthority('user:lock')")
    public ResponseEntity<ApiResponse<UserDetailResponse>> toggleEnable(
            @PathVariable UUID id, @AuthenticationPrincipal UserDetails admin) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.toggleEnable(id, admin.getUsername()), "Status aktif berhasil diperbarui"));
    }
}
