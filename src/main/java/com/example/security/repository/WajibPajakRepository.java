package com.example.security.repository;

import com.example.security.entity.WajibPajak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WajibPajakRepository
        extends JpaRepository<WajibPajak, UUID>, JpaSpecificationExecutor<WajibPajak> {

    /** Untuk endpoint /all — dropdown FE */
    @org.springframework.data.jpa.repository.Query("SELECT w FROM WajibPajak w WHERE (w.isDeleted = false OR w.isDeleted IS NULL) ORDER BY w.namaWp ASC")
    List<WajibPajak> findAllByIsDeletedFalseOrderByNamaWpAsc();

    Optional<WajibPajak> findByKodeBprdAndIsDeletedFalse(String kodeBprd);

    /** Dipakai ImportKartu di NomorServiceImpl lama: resolve WP by nama */
    Optional<WajibPajak> findByNamaWpIgnoreCaseAndIsDeletedFalse(String namaWp);
}
