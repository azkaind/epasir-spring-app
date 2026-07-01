package com.example.security.service;

import com.example.security.dto.request.KartuRequest;
import com.example.security.dto.response.KartuDetailResponse;
import com.example.security.dto.response.KartuDropdownResponse;
import com.example.security.dto.response.KartuHistoryResponse;
import com.example.security.dto.response.KartuImportPreviewItem;
import com.example.security.dto.response.KartuListResponse;
import com.example.security.dto.response.PagedResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface KartuService {

    /** List dengan search, filter idWp, dan pagination */
    PagedResponse<KartuListResponse> resolveAll(String q, String idWp,
                                                int pageSize, int pageNumber,
                                                String sortBy, String sortType);

    /** Dropdown ringan untuk select box form pengajuan */
    List<KartuDropdownResponse> getAll();

    /** Detail kartu berdasarkan UUID (untuk halaman edit) */
    KartuListResponse resolveById(UUID id);

    /** Lookup by kode_kartu / no_rfid / no_va — auto-fill form pengajuan */
    List<KartuDetailResponse> resolveByKodeOrRfid(String nomor);

    /** Riwayat topup per kartu */
    KartuHistoryResponse getHistory(String kodeKartu);

    /** CRUD */
    KartuListResponse create(KartuRequest req, String userId);
    KartuListResponse update(UUID id, KartuRequest req, String userId);
    void              softDelete(UUID id, String userId);

    /** Excel — baca file, kembalikan preview tanpa INSERT */
    List<KartuImportPreviewItem> previewImport(MultipartFile file);

    /** Excel — simpan hasil preview ke DB */
    void saveImport(List<KartuImportPreviewItem> data, String userId);

    /**
     * Import Top Up Massal — baca Excel, cek kartu yang cocok dengan idWp,
     * kembalikan list kartu untuk konfirmasi FE.
     * FE yang kemudian submit ke POST /api/bprd/pengajuan-topup.
     */
    List<KartuListResponse> importTopup(MultipartFile file, String idWp);

    /** Download template Excel kosong untuk import kartu */
    Resource downloadTemplate();
}
