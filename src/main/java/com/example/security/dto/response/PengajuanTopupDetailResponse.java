package com.example.security.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response satu baris detail kartu RFID dalam pengajuan top-up.
 * Menggabungkan t_pengajuan_topup_detail LEFT JOIN m_kartu LEFT JOIN m_komoditas.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PengajuanTopupDetailResponse {

    private String        id;
    private String        idPengajuanTopup;
    private String        kodeKartu;     // dari m_kartu
    private String        noRfid;
    private String        noVa;
    private Double        nominal;
    private String        statusApproveBjtm;
    private String        nama;          // nama komoditas dari m_komoditas
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
