package com.example.security.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Data
public class WajibPajakRequest {
    private String    kodeBprd;
    private String    kodeBjtm;

    @NotBlank(message = "namaWp tidak boleh kosong")
    private String    namaWp;

    private String    desa;
    private String    kecamatan;
    private String    iupUop;
    private String    komoditas;
    private LocalDate ijinBerlaku;

    @NotBlank(message = "npwp tidak boleh kosong")
    private String    npwp;

    private Boolean   statusPerizinan;
    private Long      noCustomer;
}
