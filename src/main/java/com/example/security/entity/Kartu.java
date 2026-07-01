package com.example.security.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "m_kartu")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class Kartu {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "varchar(36)")
    private UUID id;

    @Column(name = "no_rfid",    length = 100) private String noRfid;
    @Column(name = "kode_kartu", length = 100) private String kodeKartu;
    @Column(name = "no_va",      length = 100) private String noVa;
    @Column(name = "nama_wp",    length = 200) private String namaWp;
    @Column(name = "id_wp",      length = 36)  private String idWp;
    @Column(name = "id_komoditas", length = 36) private String idKomoditas;

    @Column(name = "saldo")      @Builder.Default private Double saldo     = 0.0;
    @Column(name = "saldo_bprd") @Builder.Default private Double saldoBprd = 0.0;

    @Column(name = "armada",     length = 255) private String armada;
    @Column(name = "nopol",      length = 10)  private String nopol;
    @Column(name = "tujuan",     length = 100) private String tujuan;
    @Column(name = "keterangan", columnDefinition = "text") private String keterangan;
    @Column(name = "aktif")      @Builder.Default private Boolean aktif    = true;
    @Column(name = "encrypt_va", columnDefinition = "text") private String encryptVa;
    @Column(name = "tgl_pendaftaran") private LocalDateTime tglPendaftaran;
    @Column(name = "porporasi")  @Builder.Default private Boolean porporasi = false;
    @Column(name = "last_topup") private LocalDateTime lastTopup;

    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "created_by", length = 36) private String createdBy;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @Column(name = "updated_by", length = 36) private String updatedBy;
    @Column(name = "is_deleted") @Builder.Default private Boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (isDeleted == null) isDeleted = false;
        if (saldo     == null) saldo     = 0.0;
        if (saldoBprd == null) saldoBprd = 0.0;
        if (aktif     == null) aktif     = true;
    }

    @PreUpdate
    public void preUpdate() { updatedAt = LocalDateTime.now(); }
}
