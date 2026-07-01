package com.example.security.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KomoditasResponse {
    private String        id;
    private String        nama;
    private Integer       nominal;
    private BigDecimal    tonase;
    private String        warnaBackground;
    private String        warnaBackgroundNama;
    private String        idKelompokKomoditas;
    private Double        nominalOpsen;
    private Double        persentaseOpsen;
    private LocalDateTime createdAt;
    private String        createdBy;
    private LocalDateTime updatedAt;
    private String        updatedBy;
    private Boolean       isDeleted;
}
