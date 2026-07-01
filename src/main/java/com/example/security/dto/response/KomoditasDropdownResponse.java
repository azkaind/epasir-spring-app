package com.example.security.dto.response;

import lombok.Builder;
import lombok.Data;

/** Dropdown untuk form kartu/topup: nominal perlu ikut karena dipakai auto-fill */
@Data @Builder
public class KomoditasDropdownResponse {
    private String  id;
    private String  nama;
    private Integer nominal;
}
