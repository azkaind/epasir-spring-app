package com.example.security.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request cek saldo Virtual Account Bank Jatim.
 */
@Data
public class CheckSaldoVaRequest {

    @NotBlank(message = "VirtualAccount tidak boleh kosong")
    private String VirtualAccount;
}
