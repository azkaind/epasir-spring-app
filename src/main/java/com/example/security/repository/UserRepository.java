package com.example.security.repository;

import com.example.security.entity.AppRole;
import com.example.security.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // ── Pencarian & Filter ────────────────────────────────────────────────
    @Query("""
        SELECT u FROM User u
        WHERE (:search IS NULL OR
               LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.email)    LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:roleId IS NULL OR u.role.id = :roleId)
          AND (:enabled IS NULL OR u.enabled = :enabled)
    """)
    Page<User> searchUsers(String search, UUID roleId, Boolean enabled, Pageable pageable);

    long countByRole(AppRole role);

    // ── Update Helpers ────────────────────────────────────────────────────
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.username = :username")
    void incrementFailedAttempts(String username);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.accountNonLocked = true, u.lockTime = null WHERE u.username = :username")
    void resetFailedAttempts(String username);
}
