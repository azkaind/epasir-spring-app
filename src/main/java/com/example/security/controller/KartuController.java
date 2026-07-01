package com.example.security.controller;

import com.example.security.dto.request.KartuRequest;
import com.example.security.dto.response.ApiResponse;
import com.example.security.dto.response.KartuDetailResponse;
import com.example.security.dto.response.KartuDropdownResponse;
import com.example.security.dto.response.KartuHistoryResponse;
import com.example.security.dto.response.KartuImportPreviewItem;
import com.example.security.dto.response.KartuListResponse;
import com.example.security.dto.response.PagedResponse;
import com.example.security.service.KartuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Base path: /api/master/kartu
 *
 * Setara dengan NomorHandler di sistem Go lama (/v1/master/kartu).
 *
 * Endpoint:
 *   GET    /                       → list + search + pagination + filter idWp
 *   GET    /all                    → dropdown (id, kodeKartu, noRfid, namaWp)
 *   GET    /{id}                   → detail by UUID
 *   GET    /lookup/{kode}          → lookup by kodeKartu / noRfid / noVa (auto-fill form)
 *   GET    /history/{kodeKartu}    → riwayat topup per kartu
 *   POST   /                       → create
 *   PUT    /{id}                   → update
 *   DELETE /{id}                   → soft delete
 *   GET    /download-template      → download template Excel kosong
 *   POST   /import                 → preview import Excel (tanpa simpan ke DB)
 *   POST   /save-import            → simpan hasil preview
 *   POST   /import-topup           → import top up massal (kembalikan list kartu cocok)
 */
@RestController
@RequestMapping("/api/master/kartu")
@RequiredArgsConstructor
public class KartuController {

    private final KartuService kartuService;

    // ── LIST ─────────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<KartuListResponse>>> resolveAll(
            @RequestParam(value = "q",          defaultValue = "")          String keyword,
            @RequestParam(value = "idWp",       defaultValue = "")          String idWp,
            @RequestParam(value = "pageSize",   defaultValue = "10")        int    pageSize,
            @RequestParam(value = "pageNumber", defaultValue = "1")         int    pageNumber,
            @RequestParam(value = "sortBy",     defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortType",   defaultValue = "DESC")      String sortType) {

        return ResponseEntity.ok(ApiResponse.success(
                kartuService.resolveAll(keyword, idWp, pageSize, pageNumber, sortBy, sortType),
                "Data berhasil diambil"));
    }

    // ── DROPDOWN ──────────────────────────────────────────────────────────────

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<KartuDropdownResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(kartuService.getAll(), "Data berhasil diambil"));
    }

    // ── DETAIL BY ID ──────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<KartuListResponse>> resolveById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(kartuService.resolveById(id), "Data berhasil diambil"));
    }

    // ── LOOKUP BY KODE / RFID / VA ────────────────────────────────────────────

    /**
     * Digunakan FE Angular untuk auto-fill form pengajuan topup.
     * Setara dengan GET /v1/master/kartu/andro/{nomor} di sistem Go lama.
     */
    @GetMapping("/lookup/{kode}")
    public ResponseEntity<ApiResponse<List<KartuDetailResponse>>> resolveByKodeOrRfid(
            @PathVariable String kode) {
        return ResponseEntity.ok(ApiResponse.success(
                kartuService.resolveByKodeOrRfid(kode),
                "Data berhasil diambil"));
    }

    // ── HISTORY ───────────────────────────────────────────────────────────────

    @GetMapping("/history/{kodeKartu}")
    public ResponseEntity<ApiResponse<KartuHistoryResponse>> getHistory(
            @PathVariable String kodeKartu) {
        return ResponseEntity.ok(ApiResponse.success(
                kartuService.getHistory(kodeKartu),
                "Riwayat kartu berhasil diambil"));
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<KartuListResponse>> create(
            @Valid @RequestBody KartuRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                kartuService.create(request, userDetails.getUsername()),
                "Kartu berhasil ditambahkan"));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<KartuListResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody KartuRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.success(
                kartuService.update(id, request, userDetails.getUsername()),
                "Kartu berhasil diupdate"));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> softDelete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        kartuService.softDelete(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("success", "Kartu berhasil dihapus"));
    }

    // ── EXCEL — DOWNLOAD TEMPLATE ─────────────────────────────────────────────

    @GetMapping("/download-template")
    public ResponseEntity<Resource> downloadTemplate() {
        Resource resource = kartuService.downloadTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"template_import_kartu.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }

    // ── EXCEL — PREVIEW IMPORT ────────────────────────────────────────────────

    /**
     * Step 1: Upload file Excel → kembalikan preview data (tanpa INSERT ke DB).
     * FE menampilkan tabel preview kepada user untuk dikonfirmasi.
     */
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<List<KartuImportPreviewItem>>> previewImport(
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(ApiResponse.success(
                kartuService.previewImport(file),
                "Preview import berhasil. Silahkan konfirmasi data."));
    }

    // ── EXCEL — SAVE IMPORT ───────────────────────────────────────────────────

    /**
     * Step 2: Setelah user konfirmasi, simpan data preview ke DB.
     */
    @PostMapping("/save-import")
    public ResponseEntity<ApiResponse<String>> saveImport(
            @RequestBody List<KartuImportPreviewItem> data,
            @AuthenticationPrincipal UserDetails userDetails) {

        kartuService.saveImport(data, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("success",
                data.size() + " data kartu berhasil diimport"));
    }

    // ── EXCEL — IMPORT TOPUP MASSAL ───────────────────────────────────────────

    /**
     * Baca Excel topup massal, kembalikan list kartu yang cocok dengan idWp.
     * Tidak membuat PengajuanTopup — FE yang submit ke POST /api/bprd/pengajuan-topup.
     */
    @PostMapping("/import-topup")
    public ResponseEntity<ApiResponse<List<KartuListResponse>>> importTopup(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "idWp", defaultValue = "") String idWp) {

        return ResponseEntity.ok(ApiResponse.success(
                kartuService.importTopup(file, idWp),
                "Import topup berhasil diproses"));
    }
}
