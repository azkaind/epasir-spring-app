package com.example.security.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "m_komoditas")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class Komoditas {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "varchar(36)")
    private UUID id;

    @Column(name = "nama", length = 100)
    private String nama;

    @Column(name = "nominal")
    private Integer nominal;

    @Column(name = "tonase")
    private BigDecimal tonase;

    @Column(name = "warna_background")
    private String warnaBackground;

    @Column(name = "warna_background_nama")
    private String warnaBackgroundNama;

    @Column(name = "id_kelompok_komoditas", length = 36)
    private String idKelompokKomoditas;

    @Column(name = "nominal_opsen")
    private Double nominalOpsen;

    @Column(name = "persentase_opsen")
    private Double persentaseOpsen;

    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "created_by", length = 36) private String createdBy;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @Column(name = "updated_by", length = 36) private String updatedBy;
    @Column(name = "is_deleted") @Builder.Default private Boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (isDeleted == null) isDeleted = false;
    }

    @PreUpdate
    public void preUpdate() { updatedAt = LocalDateTime.now(); }
}
