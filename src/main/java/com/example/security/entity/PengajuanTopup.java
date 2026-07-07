package com.example.security.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity header pengajuan top-up saldo kartu RFID.
 * Mapping ke tabel: t_pengajuan_topup
 *
 * Status: 1 = Proses, 2 = Diapprove, 3 = Ditolak
 */
@Entity
@Table(name = "t_pengajuan_topup")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class PengajuanTopup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "varchar(36)")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "tanggal")
    private LocalDate tanggal;

    /**
     * Auto-generated oleh DB function fn_get_no_topup():
     * format 0001/DD-MM/BPRD/YYYY
     * Kita set insertable=false/updatable=false agar Hibernate tidak
     * override nilai DEFAULT yang sudah di-generate DB.
     */
    @Column(name = "nomor", length = 50,
            insertable = false, updatable = false)
    private String nomor;

    @Column(name = "id_wp", length = 36)
    private String idWp;

    @Column(name = "nominal")
    private Double nominal;

    @Column(name = "file_bukti", columnDefinition = "text")
    private String fileBukti;

    /**
     * Status: 1 = Proses, 2 = Diapprove, 3 = Ditolak
     */
    @Column(name = "status", length = 2)
    @Builder.Default
    private String status = "1";

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    // Relasi ke detail dihapus karena idPengajuanTopup bukan mapping entity (menghindari error JPA)

    // ── Pre-persist / pre-update hooks ────────────────────────────────────
    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (isDeleted == null) isDeleted = false;
        if (status   == null) status   = "1";
        if (tanggal  == null) tanggal  = LocalDate.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
