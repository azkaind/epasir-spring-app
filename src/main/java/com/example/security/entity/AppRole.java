package com.example.security.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity AppRole — menggantikan enum Role sebelumnya.
 *
 * Role sekarang bersifat dinamis (disimpan di database),
 * memiliki nama, deskripsi, warna UI, dan kumpulan Permission granular.
 *
 * Hubungan:
 *   AppRole (1) ──── (*) User
 *   AppRole (*) ──── (*) Permission  [join table: role_permissions]
 *
 * Role default yang di-seed:
 *   ROLE_ADMIN, ROLE_MODERATOR, ROLE_USER
 */
@Entity
@Table(name = "app_roles", indexes = {
    @Index(name = "idx_role_name", columnList = "name", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class AppRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Nama role, diawali ROLE_ sesuai konvensi Spring Security. */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /** Deskripsi singkat untuk ditampilkan di UI. */
    @Column(length = 200)
    private String description;

    /**
     * Warna hex untuk badge di UI (mis. "#1A237E").
     * Default: biru gelap.
     */
    @Column(length = 10)
    @Builder.Default
    private String color = "#1A237E";

    /**
     * Apakah role ini sistem (tidak boleh dihapus).
     * ROLE_ADMIN, ROLE_USER adalah system role.
     */
    @Column(name = "is_system_role")
    @Builder.Default
    private boolean systemRole = false;

    /**
     * Kumpulan permission yang dimiliki role ini.
     * Disimpan sebagai string di tabel join role_permissions.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Helper methods ─────────────────────────────────────────────────────

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        this.permissions.remove(permission);
    }

    /** Kembalikan semua authority string (role + permissions) untuk Spring Security. */
    public Set<String> getAllAuthorities() {
        Set<String> authorities = new HashSet<>();
        authorities.add(this.name); // mis. "ROLE_ADMIN"
        permissions.forEach(p -> authorities.add(p.getAuthority())); // mis. "user:view"
        return authorities;
    }
}
