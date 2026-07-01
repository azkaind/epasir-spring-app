package com.example.security.dto.response;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class FileUploadResponse {
    /** Path relatif yang disimpan di kolom file_bukti di DB */
    private String path;
    /** URL untuk mengakses file via endpoint view */
    private String url;
}
