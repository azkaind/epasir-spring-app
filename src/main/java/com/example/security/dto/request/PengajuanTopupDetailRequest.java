package com.example.security.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Satu baris detail kartu RFID dalam pengajuan top-up.
 */
@Data
public class PengajuanTopupDetailRequest {

    /** Diisi saat update detail yang sudah ada; kosong = tambah baru */
    private String id;

    @NotBlank(message = "noRfid tidak boleh kosong")
    private String noRfid;

    @NotBlank(message = "noVa tidak boleh kosong")
    private String noVa;

    @NotNull(message = "nominal detail tidak boleh kosong")
    @Positive(message = "nominal detail harus positif")
    private Double nominal;

    /**
     * Status approve Bank Jatim: 1 = Proses, 2 = Approved.
     * Jika tidak diisi, service default ke "1".
     */
    private String statusApproveBjtm;
}
