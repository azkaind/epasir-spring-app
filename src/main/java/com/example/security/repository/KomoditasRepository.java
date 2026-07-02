package com.example.security.repository;

import com.example.security.entity.Komoditas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KomoditasRepository
        extends JpaRepository<Komoditas, UUID>, JpaSpecificationExecutor<Komoditas> {

    @org.springframework.data.jpa.repository.Query("SELECT k FROM Komoditas k WHERE (k.isDeleted = false OR k.isDeleted IS NULL) ORDER BY k.nama ASC")
    List<Komoditas> findAllByIsDeletedFalseOrderByNamaAsc();

    /** Dipakai ImportKartu: resolve komoditas by nama */
    Optional<Komoditas> findByNamaIgnoreCaseAndIsDeletedFalse(String nama);
}
