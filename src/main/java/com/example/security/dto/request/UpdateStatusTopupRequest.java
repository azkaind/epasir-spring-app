package com.example.security.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body untuk update status pengajuan top-up.
 * Status: 1 = Proses, 2 = Diapprove, 3 = Ditolak
 */
@Data
public class UpdateStatusTopupRequest {

    @NotBlank(message = "id tidak boleh kosong")
    private String id;

    @NotBlank(message = "status tidak boleh kosong")
    private String status;
}
