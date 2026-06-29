package com.example.security.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Request untuk proses approval top-up per kartu melalui API Bank Jatim.
 */
@Data
public class ApprovalTopupRequest {

    @NotEmpty(message = "data approval tidak boleh kosong")
    @Valid
    private List<ApprovalTopupDetailRequest> data;

    @Data
    public static class ApprovalTopupDetailRequest {

        @NotBlank(message = "id detail tidak boleh kosong")
        private String id;

        @NotBlank(message = "noRfid tidak boleh kosong")
        private String noRfid;

        @NotBlank(message = "noVa tidak boleh kosong")
        private String noVa;
    }
}
