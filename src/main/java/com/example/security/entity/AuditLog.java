package com.example.security.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity AuditLog — mencatat semua aktivitas keamanan penting.
 * Digunakan untuk forensik, compliance, dan deteksi ancaman.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user", columnList = "username"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 100)
    private String username;     // Username yang melakukan aksi

    @Column(nullable = false, length = 100)
    private String action;       // LOGIN, LOGOUT, REGISTER, ACCESS_DENIED, dll

    @Column(length = 50)
    private String ipAddress;    // IP address request

    @Column(name = "user_agent", length = 255)
    private String userAgent;    // Browser/device info

    @Column(name = "resource_path", length = 255)
    private String resourcePath; // Endpoint yang diakses

    @Column(length = 20)
    private String status;       // SUCCESS, FAILED, BLOCKED

    @Column(length = 500)
    private String details;      // Informasi tambahan

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Factory methods untuk kemudahan
    public static AuditLog success(String username, String action, String ip, String details) {
        return AuditLog.builder()
                .username(username).action(action)
                .ipAddress(ip).status("SUCCESS").details(details)
                .build();
    }

    public static AuditLog failed(String username, String action, String ip, String details) {
        return AuditLog.builder()
                .username(username).action(action)
                .ipAddress(ip).status("FAILED").details(details)
                .build();
    }
}
