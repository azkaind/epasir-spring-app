package com.example.security.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KartuRequest {

    @NotBlank(message = "noRfid tidak boleh kosong")
    private String noRfid;

    @NotBlank(message = "kodeKartu tidak boleh kosong")
    private String kodeKartu;

    private String  noVa;
    private String  namaWp;
    private String  idWp;
    private String  idKomoditas;
    private String  armada;
    private String  nopol;
    private String  tujuan;
    private String  keterangan;
    private Boolean aktif;
}
