package com.example.security.service.impl;

import com.example.security.dto.request.CreateUserRequest;
import com.example.security.dto.request.UpdateUserRequest;
import com.example.security.dto.response.UserDetailResponse;
import com.example.security.dto.response.UserStatsResponse;
import com.example.security.entity.AppRole;
import com.example.security.entity.User;
import com.example.security.exception.BadRequestException;
import com.example.security.exception.UserAlreadyExistsException;
import com.example.security.exception.UserNotFoundException;
import com.example.security.repository.AppRoleRepository;
import com.example.security.repository.RefreshTokenRepository;
import com.example.security.repository.UserRepository;
import com.example.security.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserManagementServiceImpl {

    private final UserRepository         userRepository;
    private final AppRoleRepository      roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder        passwordEncoder;
    private final AuditService           auditService;

    @Transactional(readOnly = true)
    public Page<UserDetailResponse> getUsers(String search, UUID roleId, Boolean enabled, Pageable pageable) {
        return userRepository.searchUsers(search, roleId, enabled, pageable)
                .map(u -> UserDetailResponse.from(u, 0));
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getUserById(UUID id) {
        return UserDetailResponse.from(findUserOrThrow(id), 0);
    }

    public UserDetailResponse createUser(CreateUserRequest req, String adminUsername) {
        if (userRepository.existsByUsername(req.getUsername()))
            throw new UserAlreadyExistsException("Username '" + req.getUsername() + "' sudah digunakan.");
        if (userRepository.existsByEmail(req.getEmail()))
            throw new UserAlreadyExistsException("Email '" + req.getEmail() + "' sudah terdaftar.");

        AppRole role = roleRepository.findById(req.getRoleId())
                .orElseThrow(() -> new UserNotFoundException("Role tidak ditemukan."));

        User user = User.builder()
                .username(req.getUsername()).email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName()).phoneNumber(req.getPhoneNumber())
                .role(role).build();

        user = userRepository.save(user);
        auditService.log(adminUsername, "USER_CREATED", null, "SUCCESS",
                "User '" + user.getUsername() + "' dibuat oleh admin");
        log.info("Admin '{}' membuat user '{}'", adminUsername, user.getUsername());
        return UserDetailResponse.from(user, 0);
    }

    public UserDetailResponse updateUser(UUID id, UpdateUserRequest req, String adminUsername) {
        User user = findUserOrThrow(id);
        if (req.getFullName()    != null) user.setFullName(req.getFullName());
        if (req.getPhoneNumber() != null) user.setPhoneNumber(req.getPhoneNumber());
        if (req.getEnabled()     != null) user.setEnabled(req.getEnabled());

        if (req.getEmail() != null && !req.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(req.getEmail()))
                throw new UserAlreadyExistsException("Email sudah digunakan.");
            user.setEmail(req.getEmail());
        }
        if (req.getNewPassword() != null && !req.getNewPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.getNewPassword()));
            refreshTokenRepository.revokeAllUserTokens(user);
            auditService.log(adminUsername, "ADMIN_RESET_PASSWORD", null, "SUCCESS",
                    "Password '" + user.getUsername() + "' direset oleh admin");
        }
        if (req.getRoleId() != null) {
            AppRole newRole = roleRepository.findById(req.getRoleId())
                    .orElseThrow(() -> new UserNotFoundException("Role tidak ditemukan."));
            String oldRole = user.getRoleName();
            user.setRole(newRole);
            auditService.log(adminUsername, "ROLE_CHANGED", null, "SUCCESS",
                    "Role '" + user.getUsername() + "': " + oldRole + " → " + newRole.getName());
        }
        user = userRepository.save(user);
        log.info("Admin '{}' memperbarui user '{}'", adminUsername, user.getUsername());
        return UserDetailResponse.from(user, 0);
    }

    public void deleteUser(UUID id, String adminUsername) {
        User user = findUserOrThrow(id);
        if (user.getUsername().equals(adminUsername))
            throw new BadRequestException("Anda tidak dapat menghapus akun Anda sendiri.");
        refreshTokenRepository.revokeAllUserTokens(user);
        userRepository.delete(user);
        auditService.log(adminUsername, "USER_DELETED", null, "SUCCESS",
                "User '" + user.getUsername() + "' dihapus oleh admin");
        log.info("Admin '{}' menghapus user '{}'", adminUsername, user.getUsername());
    }

    public UserDetailResponse assignRole(UUID userId, UUID roleId, String adminUsername) {
        User user = findUserOrThrow(userId);
        AppRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new UserNotFoundException("Role tidak ditemukan."));
        String oldRole = user.getRoleName();
        user.setRole(role);
        user = userRepository.save(user);
        refreshTokenRepository.revokeAllUserTokens(user);
        auditService.log(adminUsername, "ROLE_ASSIGNED", null, "SUCCESS",
                "User '" + user.getUsername() + "': " + oldRole + " → " + role.getName());
        return UserDetailResponse.from(user, 0);
    }

    public UserDetailResponse toggleLock(UUID id, String adminUsername) {
        User user = findUserOrThrow(id);
        if (user.getUsername().equals(adminUsername))
            throw new BadRequestException("Anda tidak dapat mengunci akun Anda sendiri.");
        boolean wasLocked = !user.isAccountNonLocked();
        user.setAccountNonLocked(wasLocked);
        if (wasLocked) { user.setFailedLoginAttempts(0); user.setLockTime(null); }
        else { user.setLockTime(LocalDateTime.now()); refreshTokenRepository.revokeAllUserTokens(user); }
        user = userRepository.save(user);
        auditService.log(adminUsername, wasLocked ? "ACCOUNT_UNLOCKED" : "ACCOUNT_LOCKED",
                null, "SUCCESS", "Akun '" + user.getUsername() + "' oleh admin");
        return UserDetailResponse.from(user, 0);
    }

    public UserDetailResponse toggleEnable(UUID id, String adminUsername) {
        User user = findUserOrThrow(id);
        if (user.getUsername().equals(adminUsername))
            throw new BadRequestException("Anda tidak dapat menonaktifkan akun Anda sendiri.");
        user.setEnabled(!user.isEnabled());
        if (!user.isEnabled()) refreshTokenRepository.revokeAllUserTokens(user);
        user = userRepository.save(user);
        auditService.log(adminUsername, user.isEnabled() ? "ACCOUNT_ENABLED" : "ACCOUNT_DISABLED",
                null, "SUCCESS", "Akun '" + user.getUsername() + "' oleh admin");
        return UserDetailResponse.from(user, 0);
    }

    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats() {
        List<User> all = userRepository.findAll();
        Map<String, Long> byRole = all.stream()
                .filter(u -> u.getRole() != null)
                .collect(Collectors.groupingBy(u -> u.getRole().getName(), Collectors.counting()));
        return UserStatsResponse.builder()
                .totalUsers((long) all.size())
                .activeUsers(all.stream().filter(u -> u.isEnabled() && u.isAccountNonLocked()).count())
                .lockedUsers(all.stream().filter(u -> !u.isAccountNonLocked()).count())
                .disabledUsers(all.stream().filter(u -> !u.isEnabled()).count())
                .usersByRole(byRole).build();
    }

    private User findUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Pengguna tidak ditemukan: " + id));
    }
}
