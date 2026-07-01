package com.example.security.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "m_wajib_pajak")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class WajibPajak {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "varchar(36)")
    private UUID id;

    @Column(name = "kode_bprd", length = 100)
    private String kodeBprd;

    @Column(name = "kode_bjtm", length = 100)
    private String kodeBjtm;

    @Column(name = "nama_wp", length = 200)
    private String namaWp;

    @Column(name = "desa", length = 200)
    private String desa;

    @Column(name = "kecamatan", length = 200)
    private String kecamatan;

    @Column(name = "iup_uop", length = 200)
    private String iupUop;

    /** Teks bebas, BUKAN FK ke m_komoditas */
    @Column(name = "komoditas", length = 200)
    private String komoditas;

    @Column(name = "ijin_berlaku")
    private LocalDate ijinBerlaku;

    @Column(name = "npwp", length = 100)
    private String npwp;

    @Column(name = "status_perizinan")
    @Builder.Default
    private Boolean statusPerizinan = true;

    @Column(name = "no_customer")
    private Long noCustomer;

    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "created_by", length = 36) private String createdBy;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @Column(name = "updated_by", length = 36) private String updatedBy;
    @Column(name = "is_deleted") @Builder.Default private Boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        if (createdAt == null)      createdAt      = LocalDateTime.now();
        if (isDeleted == null)      isDeleted      = false;
        if (statusPerizinan == null) statusPerizinan = true;
    }

    @PreUpdate
    public void preUpdate() { updatedAt = LocalDateTime.now(); }
}
