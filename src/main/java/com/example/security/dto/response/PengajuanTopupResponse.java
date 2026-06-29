package com.example.security.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO untuk pengajuan top-up (list & detail).
 * Menggabungkan data dari t_pengajuan_topup JOIN m_wajib_pajak,
 * plus agregasi dari t_pengajuan_topup_detail.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PengajuanTopupResponse {

    private String          id;
    private LocalDate       tanggal;
    private String          nomor;
    private String          idWp;
    private Double          nominal;
    private String          fileBukti;
    private LocalDateTime   createdAt;
    private String          createdBy;
    private LocalDateTime   updatedAt;
    private String          updatedBy;
    private Boolean         isDeleted;
    private String          status;
    private LocalDateTime   verifiedAt;

    // ── Join dari m_wajib_pajak ──────────────────────────────────────────
    private String namaWp;
    private String iupUop;
    private String npwp;

    // ── Agregasi detail ──────────────────────────────────────────────────
    /** Jumlah kartu dengan status_approve_bjtm = '1' (belum ditopup) */
    private Integer jmlBelumTopup;
    /** Jumlah kartu dengan status_approve_bjtm = '2' (sudah ditopup) */
    private Integer jmlSudahTopup;
    /** Total kartu (jmlBelumTopup + jmlSudahTopup) */
    private Integer totalTopup;

    // ── Detail kartu ─────────────────────────────────────────────────────
    private List<PengajuanTopupDetailResponse> detail;
}
