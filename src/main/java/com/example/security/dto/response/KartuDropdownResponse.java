package com.example.security.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KartuDropdownResponse {
    private String id;
    private String kodeKartu;
    private String noRfid;
    private String namaWp;
}
