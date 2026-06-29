package com.example.security.repository;

import com.example.security.entity.AppRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppRoleRepository extends JpaRepository<AppRole, UUID> {

    Optional<AppRole> findByName(String name);

    boolean existsByName(String name);

    /** Role yang bukan system role — bisa dihapus. */
    List<AppRole> findBySystemRoleFalse();

    /** Semua role beserta jumlah user yang menggunakannya. */
    @Query("""
        SELECT r, COUNT(u.id) as userCount
        FROM AppRole r
        LEFT JOIN User u ON u.role = r
        GROUP BY r
        ORDER BY r.name
    """)
    List<Object[]> findAllWithUserCount();
}
