package com.example.security.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KartuDetailResponse {
    private String        id;
    private String        noRfid;
    private String        kodeKartu;
    private String        noVa;
    private String        namaWp;
    private String        idWp;
    private String        idKomoditas;
    private String        namaKomoditas;
    private Integer       nominal;
    private Double        saldo;
    private Double        saldoBprd;
    private String        armada;
    private String        nopol;
    private String        tujuan;
    private String        keterangan;
    private String        kodeBprd;
    private String        kodeBjtm;
    private LocalDateTime terakhirTopup;
}
