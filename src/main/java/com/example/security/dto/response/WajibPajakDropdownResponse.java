package com.example.security.dto.response;

import lombok.Builder;
import lombok.Data;

/** Payload ringan untuk dropdown FE — hanya field yang dibutuhkan select box */
@Data @Builder
public class WajibPajakDropdownResponse {
    private String id;
    private String namaWp;
    private String kodeBprd;
}
