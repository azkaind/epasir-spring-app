package com.example.security.service.impl;

import com.example.security.dto.request.LoginRequest;
import com.example.security.dto.request.RegisterRequest;
import com.example.security.dto.response.AuthResponse;
import com.example.security.entity.AppRole;
import com.example.security.entity.RefreshToken;
import com.example.security.entity.User;
import com.example.security.exception.BadRequestException;
import com.example.security.exception.InvalidTokenException;
import com.example.security.exception.UserAlreadyExistsException;
import com.example.security.repository.AppRoleRepository;
import com.example.security.repository.RefreshTokenRepository;
import com.example.security.repository.UserRepository;
import com.example.security.service.AuditService;
import com.example.security.service.AuthService;
import com.example.security.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository         userRepository;
    private final AppRoleRepository      roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder        passwordEncoder;
    private final AuthenticationManager  authenticationManager;
    private final JwtUtil                jwtUtil;
    private final AuditService           auditService;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;
    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;
    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;
    @Value("${app.security.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes;

    @Override
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        if (userRepository.existsByUsername(request.getUsername()))
            throw new UserAlreadyExistsException("Username '" + request.getUsername() + "' sudah digunakan.");
        if (userRepository.existsByEmail(request.getEmail()))
            throw new UserAlreadyExistsException("Email '" + request.getEmail() + "' sudah terdaftar.");

        // Ambil role default ROLE_USER dari database
        AppRole defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new BadRequestException("Role default tidak ditemukan. Jalankan schema_v2.sql."));

        User user = User.builder()
                .username(request.getUsername()).email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName()).role(defaultRole).build();

        userRepository.save(user);
        auditService.log(user.getUsername(), "REGISTER", ip, "SUCCESS", "Registrasi berhasil");

        String accessToken  = jwtUtil.generateAccessToken(user);
        String refreshToken = createRefreshToken(user, httpRequest.getHeader("User-Agent"));
        return AuthResponse.of(accessToken, refreshToken, accessTokenExpiration / 1000,
                user.getUsername(), user.getEmail(), user.getRoleName());
    }

    @Override
    @Transactional(noRollbackFor = {BadCredentialsException.class, LockedException.class})
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    auditService.log(request.getUsername(), "LOGIN", ip, "FAILED", "Username tidak ditemukan");
                    return new BadCredentialsException("Username atau password tidak valid");
                });
        checkAccountLockout(user, ip);
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException e) {
            handleFailedLogin(user, ip);
            throw e;
        }
        userRepository.resetFailedAttempts(user.getUsername());
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        refreshTokenRepository.revokeAllUserTokens(user);

        String accessToken  = jwtUtil.generateAccessToken(user);
        String refreshToken = createRefreshToken(user, httpRequest.getHeader("User-Agent"));
        auditService.log(user.getUsername(), "LOGIN", ip, "SUCCESS", "Login berhasil");
        return AuthResponse.of(accessToken, refreshToken, accessTokenExpiration / 1000,
                user.getUsername(), user.getEmail(), user.getRoleName());
    }

    @Override
    public AuthResponse refreshToken(String refreshToken, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("Refresh token tidak valid."));
        if (!stored.isValid())
            throw new InvalidTokenException("Refresh token telah expired atau dicabut. Silakan login ulang.");
        User user = stored.getUser();
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        String newAccess  = jwtUtil.generateAccessToken(user);
        String newRefresh = createRefreshToken(user, httpRequest.getHeader("User-Agent"));
        auditService.log(user.getUsername(), "TOKEN_REFRESH", ip, "SUCCESS", "Token diperbarui");
        return AuthResponse.of(newAccess, newRefresh, accessTokenExpiration / 1000,
                user.getUsername(), user.getEmail(), user.getRoleName());
    }

    @Override
    public void logout(String refreshToken, String username, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        refreshTokenRepository.findByToken(refreshToken).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
        auditService.log(username, "LOGOUT", ip, "SUCCESS", "Logout berhasil");
    }

    private String createRefreshToken(User user, String userAgent) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString()).user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .deviceInfo(userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 255)) : null)
                .build();
        return refreshTokenRepository.save(token).getToken();
    }

    private void checkAccountLockout(User user, String ip) {
        if (!user.isAccountNonLocked() && user.getLockTime() != null) {
            LocalDateTime unlockTime = user.getLockTime().plusMinutes(lockoutDurationMinutes);
            if (LocalDateTime.now().isBefore(unlockTime)) {
                auditService.log(user.getUsername(), "LOGIN", ip, "BLOCKED", "Akun terkunci");
                throw new LockedException("Akun dikunci. Coba lagi pada " + unlockTime);
            } else {
                // Masa kunci sudah berakhir — reset di DB dan update objek lokal
                user.setAccountNonLocked(true);
                user.setFailedLoginAttempts(0);
                user.setLockTime(null);
                userRepository.save(user);
            }
        }
    }

    private void handleFailedLogin(User user, String ip) {
        // Increment langsung pada objek entity (bukan query @Modifying terpisah)
        // agar nilai terbaru tidak tertimpa saat save() dipanggil
        int newAttempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(newAttempts);

        if (newAttempts >= maxLoginAttempts) {
            user.setAccountNonLocked(false);
            user.setLockTime(LocalDateTime.now());
            userRepository.save(user);
            auditService.log(user.getUsername(), "ACCOUNT_LOCKED", ip, "SYSTEM",
                    "Akun dikunci setelah " + newAttempts + " percobaan gagal");
        } else {
            userRepository.save(user);
            auditService.log(user.getUsername(), "LOGIN", ip, "FAILED",
                    "Percobaan ke-" + newAttempts + " dari " + maxLoginAttempts);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getRemoteAddr();
        return ip.split(",")[0].trim();
    }
}
