package com.example.security.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Entity User — diperbarui untuk menggunakan AppRole (ManyToOne)
 * sebagai pengganti enum Role sebelumnya.
 *
 * getAuthorities() sekarang mengembalikan ROLE + semua permission granular
 * dari AppRole yang ditetapkan, sehingga @PreAuthorize("hasAuthority('user:view')")
 * bekerja langsung di controller.
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email",    columnList = "email",    unique = true),
    @Index(name = "idx_users_username", columnList = "username", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    // ── Role (ManyToOne → AppRole) ─────────────────────────────────────────
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private AppRole role;

    // ── Status ─────────────────────────────────────────────────────────────
    @Builder.Default private boolean enabled          = true;
    @Builder.Default private boolean accountNonLocked = true;
    @Column(name = "failed_login_attempts")
    @Builder.Default private int failedLoginAttempts  = 0;
    @Column(name = "lock_time") private LocalDateTime lockTime;
    @Column(name = "last_login") private LocalDateTime lastLogin;

    // ── Audit ──────────────────────────────────────────────────────────────
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── UserDetails ────────────────────────────────────────────────────────

    /**
     * Mengembalikan SEMUA authority: role name + setiap permission granular.
     * Contoh untuk ROLE_ADMIN: ["ROLE_ADMIN", "user:view", "user:create", ...]
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) return Set.of();

        return role.getAllAuthorities().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    @Override public boolean isAccountNonExpired()   { return true; }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isAccountNonLocked()    { return accountNonLocked; }
    @Override public boolean isEnabled()             { return enabled; }

    // ── Convenience ────────────────────────────────────────────────────────

    public String getRoleName() {
        return role != null ? role.getName() : "ROLE_USER";
    }

    public boolean hasPermission(Permission permission) {
        return role != null && role.hasPermission(permission);
    }
}
