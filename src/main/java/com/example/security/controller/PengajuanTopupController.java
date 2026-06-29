package com.example.security.controller;

import com.example.security.dto.request.ApprovalTopupRequest;
import com.example.security.dto.request.CheckSaldoVaRequest;
import com.example.security.dto.request.PengajuanTopupRequest;
import com.example.security.dto.request.UpdateStatusTopupRequest;
import com.example.security.dto.response.ApiResponse;
import com.example.security.dto.response.PagedResponse;
import com.example.security.dto.response.PengajuanTopupDetailResponse;
import com.example.security.dto.response.PengajuanTopupResponse;
import com.example.security.service.PengajuanTopupService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller untuk fitur Pengajuan Top-Up.
 *
 * Base path: /api/bprd/pengajuan-topup
 *
 * Endpoint yang tersedia (persis seperti sistem Go lama):
 *  GET    /                → list dengan pagination & filter
 *  GET    /{id}            → detail satu record
 *  POST   /                → buat pengajuan baru
 *  PUT    /{id}            → update pengajuan
 *  DELETE /{id}            → soft-delete
 *  POST   /update-status   → approve / tolak pengajuan
 *  POST   /approval        → approval per kartu via Bank Jatim
 *  POST   /cek-saldo       → cek saldo VA Bank Jatim
 */
@RestController
@RequestMapping("/api/bprd/pengajuan-topup")
@RequiredArgsConstructor
@Slf4j
public class PengajuanTopupController {

    private final PengajuanTopupService topupService;
    private final ObjectMapper          objectMapper;

    // ── GET /  ───────────────────────────────────────────────────────────
    /**
     * Ambil list pengajuan top-up dengan filter, sorting, dan pagination.
     *
     * Query params:
     *  - q          : keyword pencarian (nama WP, nomor, nominal)
     *  - pageSize   : jumlah data per halaman (wajib)
     *  - pageNumber : halaman yang diminta (wajib)
     *  - sortBy     : kolom urutan (default: createdAt)
     *  - sortType   : ASC/DESC (default: DESC)
     *  - startDate  : filter tanggal mulai (yyyy-MM-dd)
     *  - endDate    : filter tanggal akhir (yyyy-MM-dd)
     *  - idWp       : filter berdasarkan wajib pajak
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PengajuanTopupResponse>>> resolveAll(
            @RequestParam(value = "q",          defaultValue = "")       String keyword,
            @RequestParam(value = "pageSize",   defaultValue = "10")     int    pageSize,
            @RequestParam(value = "pageNumber", defaultValue = "1")      int    pageNumber,
            @RequestParam(value = "sortBy",     defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortType",   defaultValue = "DESC")   String sortType,
            @RequestParam(value = "startDate",  defaultValue = "")       String startDate,
            @RequestParam(value = "endDate",    defaultValue = "")       String endDate,
            @RequestParam(value = "idWp",       defaultValue = "")       String idWp) {

        PagedResponse<PengajuanTopupResponse> data = topupService.resolveAll(
                keyword, startDate, endDate, idWp,
                sortBy, sortType, pageSize, pageNumber);

        return ResponseEntity.ok(ApiResponse.success(data, "Data berhasil diambil"));
    }

    // ── GET /{id}  ───────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PengajuanTopupResponse>> resolveById(
            @PathVariable UUID id) {

        PengajuanTopupResponse data = topupService.resolveById(id);
        return ResponseEntity.ok(ApiResponse.success(data, "Data berhasil diambil"));
    }

    // ── POST /  ──────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<ApiResponse<PengajuanTopupResponse>> create(
            @Valid @RequestBody PengajuanTopupRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String createdBy = userDetails.getUsername();
        PengajuanTopupResponse data = topupService.create(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, "Pengajuan top-up berhasil dibuat"));
    }

    // ── PUT /{id}  ───────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PengajuanTopupResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody PengajuanTopupRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        request.setId(id);
        String updatedBy = userDetails.getUsername();
        PengajuanTopupResponse data = topupService.update(request, updatedBy);
        return ResponseEntity.ok(ApiResponse.success(data, "Pengajuan top-up berhasil diupdate"));
    }

    // ── DELETE /{id}  ────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        String deletedBy = userDetails.getUsername();
        topupService.softDelete(id, deletedBy);
        return ResponseEntity.ok(ApiResponse.success("success", "Data berhasil dihapus"));
    }

    // ── POST /update-status  ─────────────────────────────────────────────
    /**
     * Update status pengajuan: 1 = Proses, 2 = Diapprove, 3 = Ditolak.
     */
    @PostMapping("/update-status")
    public ResponseEntity<ApiResponse<String>> updateStatus(
            @Valid @RequestBody UpdateStatusTopupRequest request) {

        topupService.updateStatus(request);
        return ResponseEntity.ok(ApiResponse.success("success", "Status berhasil diupdate"));
    }

    // ── POST /approval  ──────────────────────────────────────────────────
    /**
     * Approval per kartu RFID via cek saldo VA Bank Jatim.
     * Jika saldo VA > 0 dan status kartu masih "1", ubah ke "2".
     */
    @PostMapping("/approval")
    public ResponseEntity<ApiResponse<List<PengajuanTopupDetailResponse>>> approval(
            @Valid @RequestBody ApprovalTopupRequest request) {

        List<PengajuanTopupDetailResponse> data = topupService.approvalTopup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, "Proses approval selesai"));
    }

    // ── POST /cek-saldo  ─────────────────────────────────────────────────
    /**
     * Proxy ke API Bank Jatim untuk cek saldo Virtual Account.
     */
    @PostMapping("/cek-saldo")
    public ResponseEntity<ApiResponse<JsonNode>> checkSaldoVa(
            @Valid @RequestBody CheckSaldoVaRequest request) {

        String raw = topupService.checkSaldoVa(request);
        try {
            JsonNode json = objectMapper.readTree(raw);
            return ResponseEntity.ok(ApiResponse.success(json, "Berhasil cek saldo VA"));
        } catch (Exception e) {
            log.error("Gagal parse response Bank Jatim: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(ApiResponse.error("Gagal parse response Bank Jatim", e.getMessage()));
        }
    }
}
