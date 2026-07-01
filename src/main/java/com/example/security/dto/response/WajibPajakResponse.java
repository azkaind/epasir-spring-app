package com.example.security.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WajibPajakResponse {
    private String        id;
    private String        kodeBprd;
    private String        kodeBjtm;
    private String        namaWp;
    private String        desa;
    private String        kecamatan;
    private String        iupUop;
    private String        komoditas;
    private LocalDate     ijinBerlaku;
    private String        npwp;
    private Boolean       statusPerizinan;
    private Long          noCustomer;
    private LocalDateTime createdAt;
    private String        createdBy;
    private LocalDateTime updatedAt;
    private String        updatedBy;
    private Boolean       isDeleted;
}
