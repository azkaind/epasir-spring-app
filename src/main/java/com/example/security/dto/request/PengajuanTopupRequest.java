package com.example.security.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

/**
 * Request body untuk create / update pengajuan top-up.
 * Ketika id diisi → update; kosong → create baru.
 */
@Data
public class PengajuanTopupRequest {

    /** Diisi saat update, kosong saat create */
    private String id;

    /** Format: yyyy-MM-dd; jika kosong diisi tanggal hari ini oleh service */
    private String tanggal;

    @NotBlank(message = "idWp tidak boleh kosong")
    private String idWp;

    @NotNull(message = "nominal tidak boleh kosong")
    @Positive(message = "nominal harus bernilai positif")
    private Double nominal;

    /** Path/URL file bukti transfer bank (opsional) */
    private String fileBukti;

    /**
     * Status override (opsional). Default "1" = Proses.
     * Biasanya hanya diisi oleh admin saat approval.
     */
    private String status;

    @NotEmpty(message = "detail tidak boleh kosong")
    @Valid
    private List<PengajuanTopupDetailRequest> detail;
}
