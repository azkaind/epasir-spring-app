package com.example.security.repository;

import com.example.security.entity.PengajuanTopupDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PengajuanTopupDetailRepository extends JpaRepository<PengajuanTopupDetail, UUID> {

    List<PengajuanTopupDetail> findByIdPengajuanTopupOrderByNoRfidAsc(String idPengajuanTopup);

    // ── Delete detail yang tidak ada dalam list ID (untuk update bulk) ───
    @Modifying
    @Query(value = """
        DELETE FROM t_pengajuan_topup_detail
        WHERE id_pengajuan_topup = :idTopup
          AND id NOT IN (:ids)
        """, nativeQuery = true)
    void deleteNotIn(@Param("idTopup") String idTopup,
                     @Param("ids")    List<String> ids);

    // ── Delete semua detail suatu pengajuan (dipakai saat update bulk) ───
    @Modifying
    @Query("DELETE FROM PengajuanTopupDetail d WHERE d.idPengajuanTopup = :idTopup")
    void deleteByIdPengajuanTopup(@Param("idTopup") String idTopup);

    // ── Update status approve Bank Jatim ─────────────────────────────────
    @Modifying
    @Query("""
        UPDATE PengajuanTopupDetail d
        SET d.statusApproveBjtm = :status,
            d.updatedAt         = :updatedAt
        WHERE d.id = :id
        """)
    void updateStatusApproveBjtm(@Param("id")        UUID id,
                                 @Param("status")    String status,
                                 @Param("updatedAt") LocalDateTime updatedAt);

    // ── Update saldo_bprd di m_kartu ─────────────────────────────────────
    @Modifying
    @Query(value = """
        UPDATE m_kartu
        SET saldo_bprd = :saldoBprd,
            updated_at = :updatedAt,
            updated_by = :updatedBy
        WHERE no_rfid = :noRfid
        """, nativeQuery = true)
    void updateSaldoBprd(@Param("noRfid")     String noRfid,
                         @Param("saldoBprd")  Double saldoBprd,
                         @Param("updatedAt")  LocalDateTime updatedAt,
                         @Param("updatedBy")  String updatedBy);
}
