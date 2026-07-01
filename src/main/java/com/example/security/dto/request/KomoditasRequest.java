package com.example.security.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class KomoditasRequest {
    @NotBlank(message = "nama tidak boleh kosong")
    private String     nama;

    @NotNull(message = "nominal tidak boleh kosong")
    private Integer    nominal;

    private BigDecimal tonase;
    private String     warnaBackground;
    private String     warnaBackgroundNama;
    private String     idKelompokKomoditas;
    private Double     nominalOpsen;
    private Double     persentaseOpsen;
}
