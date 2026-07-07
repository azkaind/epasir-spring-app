package com.example.security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity detail pengajuan top-up per kartu RFID.
 * Mapping ke tabel: t_pengajuan_topup_detail
 *
 * statusApproveBjtm: 1 = Proses, 2 = Sudah Ditopup, 3 = Ditolak
 */
@Entity
@Table(name = "t_pengajuan_topup_detail")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PengajuanTopupDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "varchar(36)")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    private UUID id;

    /**
     * FK ke t_pengajuan_topup.id (disimpan sebagai String agar query
     * native/JPQL tidak perlu join penuh setiap saat).
     */
    @Column(name = "id_pengajuan_topup", length = 36)
    private String idPengajuanTopup;

    @Column(name = "no_rfid", length = 100)
    private String noRfid;

    @Column(name = "no_va", length = 100)
    private String noVa;

    @Column(name = "nominal")
    private Double nominal;

    /**
     * 1 = Proses (belum ditopup Bank Jatim)
     * 2 = Sudah Ditopup (approved Bank Jatim)
     */
    @Column(name = "status_approve_bjtm", length = 10)
    @Builder.Default
    private String statusApproveBjtm = "1";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // kode_kartu & nama: tidak ada kolom di tabel ini, diambil via join
    // dari m_kartu dan m_komoditas. Ditampung di DTO, bukan entity.

    @PrePersist
    public void prePersist() {
        if (createdAt          == null) createdAt          = LocalDateTime.now();
        if (statusApproveBjtm  == null) statusApproveBjtm  = "1";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
