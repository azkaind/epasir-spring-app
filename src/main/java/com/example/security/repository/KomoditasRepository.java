package com.example.security.repository;

import com.example.security.entity.Komoditas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KomoditasRepository
        extends JpaRepository<Komoditas, UUID>, JpaSpecificationExecutor<Komoditas> {

    List<Komoditas> findAllByIsDeletedFalseOrderByNamaAsc();

    /** Dipakai ImportKartu: resolve komoditas by nama */
    Optional<Komoditas> findByNamaIgnoreCaseAndIsDeletedFalse(String nama);
}
