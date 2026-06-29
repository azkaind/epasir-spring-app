package com.example.security.controller;

import com.example.security.dto.response.ApiResponse;
import com.example.security.entity.User;
import com.example.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        Map<String, Object> profile = Map.of(
            "username",  user.getUsername(),
            "email",     user.getEmail(),
            "fullName",  user.getFullName()  != null ? user.getFullName()  : "",
            "role",      user.getRoleName(),
            "lastLogin", user.getLastLogin() != null ? user.getLastLogin().toString() : "N/A"
        );
        return ResponseEntity.ok(ApiResponse.success(profile, "Profil berhasil dimuat"));
    }

    @GetMapping("/moderator/dashboard")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MODERATOR')")
    public ResponseEntity<ApiResponse<String>> moderatorDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Data moderator dashboard", "OK"));
    }
}
