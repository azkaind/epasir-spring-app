package com.example.security.repository;

import com.example.security.entity.PengajuanTopup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface PengajuanTopupRepository extends JpaRepository<PengajuanTopup, UUID> {

    // ── Soft delete ──────────────────────────────────────────────────────
    @Modifying
    @Query("""
        UPDATE PengajuanTopup pt
        SET pt.isDeleted  = true,
            pt.updatedAt  = :updatedAt,
            pt.updatedBy  = :updatedBy
        WHERE pt.id = :id
        """)
    void softDelete(@Param("id")        UUID id,
                    @Param("updatedAt") LocalDateTime updatedAt,
                    @Param("updatedBy") String updatedBy);

    // ── Update status ────────────────────────────────────────────────────
    @Modifying
    @Query("""
        UPDATE PengajuanTopup pt
        SET pt.status     = :status,
            pt.verifiedAt = :verifiedAt,
            pt.updatedAt  = :updatedAt
        WHERE pt.id = :id
        """)
    void updateStatus(@Param("id")         UUID id,
                      @Param("status")     String status,
                      @Param("verifiedAt") LocalDateTime verifiedAt,
                      @Param("updatedAt")  LocalDateTime updatedAt);
}
