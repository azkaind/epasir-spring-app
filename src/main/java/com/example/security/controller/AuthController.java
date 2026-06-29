package com.example.security.controller;

import com.example.security.dto.request.LoginRequest;
import com.example.security.dto.request.RegisterRequest;
import com.example.security.dto.response.ApiResponse;
import com.example.security.dto.response.AuthResponse;
import com.example.security.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller untuk endpoint autentikasi.
 * Semua endpoint di sini bersifat publik (dikecualikan dari JWT filter).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Mendaftarkan user baru.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        AuthResponse response = authService.register(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Registrasi berhasil"));
    }

    /**
     * POST /api/auth/login
     * Login dan dapatkan JWT access + refresh token.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Login berhasil"));
    }

    /**
     * POST /api/auth/refresh
     * Perbarui access token menggunakan refresh token.
     * Body: { "refreshToken": "..." }
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestBody java.util.Map<String, String> body,
            HttpServletRequest httpRequest) {

        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Refresh token diperlukan", "MISSING_TOKEN"));
        }

        AuthResponse response = authService.refreshToken(refreshToken, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Token berhasil diperbarui"));
    }

    /**
     * POST /api/auth/logout
     * Logout dan invalidasi refresh token.
     * Memerlukan autentikasi (access token valid).
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {

        String refreshToken = body.get("refreshToken");
        authService.logout(refreshToken, userDetails.getUsername(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Logout berhasil"));
    }
}
