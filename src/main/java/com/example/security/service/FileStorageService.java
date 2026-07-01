package com.example.security.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    /** Simpan file, kembalikan path relatif (mis. "bukti-transfer/uuid_namafile.jpg") */
    String store(MultipartFile file, String subDir);

    /** Load file sebagai Resource untuk diserve via HTTP */
    Resource load(String relativePath);

    void delete(String relativePath);
}
