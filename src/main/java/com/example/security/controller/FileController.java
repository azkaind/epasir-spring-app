package com.example.security.controller;

import com.example.security.dto.response.ApiResponse;
import com.example.security.dto.response.FileUploadResponse;
import com.example.security.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Base path: /api/files
 *
 * Setara dengan FileHandler.ReadFile (GET /files?path=...) di sistem Go lama.
 *
 * Upload: POST /api/files/upload?subDir=bukti-transfer
 *   → multipart/form-data, field "file"
 *   → returns { path, url }
 *
 * View: GET /api/files/view?path=bukti-transfer/uuid_xxx.jpg
 *   → serve file langsung
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileStorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "subDir", defaultValue = "bukti-transfer") String subDir) {

        String path = storageService.store(file, subDir);
        FileUploadResponse resp = FileUploadResponse.builder()
                .path(path)
                .url("/api/files/view?path=" + path)
                .build();
        return ResponseEntity.ok(ApiResponse.success(resp, "File berhasil diupload"));
    }

    /**
     * Serve file — tidak memerlukan JWT jika diakses langsung dari <img src>.
     * Untuk proteksi tambahkan /api/files/view ke permitAll() atau tetap di authenticated().
     * Saat ini: authenticated() (default anyRequest di SecurityConfig).
     */
    @GetMapping("/view")
    public ResponseEntity<Resource> view(@RequestParam("path") String relativePath) {
        Resource resource = storageService.load(relativePath);
        String contentType = resolveContentType(relativePath);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    private String resolveContentType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".pdf"))  return "application/pdf";
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
