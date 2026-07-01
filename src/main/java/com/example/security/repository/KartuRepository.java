package com.example.security.repository;

import com.example.security.entity.Kartu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KartuRepository
        extends JpaRepository<Kartu, UUID>, JpaSpecificationExecutor<Kartu> {

    Optional<Kartu> findByKodeKartuAndIsDeletedFalse(String kodeKartu);
    Optional<Kartu> findByNoRfidAndIsDeletedFalse(String noRfid);
    Optional<Kartu> findByNoVaAndIsDeletedFalse(String noVa);
    boolean existsByNoRfidAndIsDeletedFalse(String noRfid);
    boolean existsByKodeKartuIgnoreCaseAndIsDeletedFalse(String kodeKartu);

    /** Untuk endpoint /all — dropdown form pengajuan */
    List<Kartu> findAllByIsDeletedFalseOrderByKodeKartuAsc();

    /** Update saldo_bprd saat create pengajuan topup — dipanggil dari PengajuanTopupServiceImpl */
    @Modifying
    @Query(value = """
        UPDATE m_kartu
        SET saldo_bprd  = COALESCE(saldo_bprd, 0) + :nominal,
            last_topup  = :now,
            updated_at  = :now,
            updated_by  = :updatedBy
        WHERE no_rfid = :noRfid AND COALESCE(is_deleted, false) = false
        """, nativeQuery = true)
    void addSaldoBprd(String noRfid, Double nominal, LocalDateTime now, String updatedBy);
}
