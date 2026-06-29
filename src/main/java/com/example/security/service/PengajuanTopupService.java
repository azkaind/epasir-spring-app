package com.example.security.service;

import com.example.security.dto.request.ApprovalTopupRequest;
import com.example.security.dto.request.CheckSaldoVaRequest;
import com.example.security.dto.request.PengajuanTopupRequest;
import com.example.security.dto.request.UpdateStatusTopupRequest;
import com.example.security.dto.response.PagedResponse;
import com.example.security.dto.response.PengajuanTopupDetailResponse;
import com.example.security.dto.response.PengajuanTopupResponse;

import java.util.List;
import java.util.UUID;

public interface PengajuanTopupService {

    /**
     * Ambil list pengajuan top-up dengan filter, sorting, dan pagination.
     */
    PagedResponse<PengajuanTopupResponse> resolveAll(String keyword,
                                                     String startDate,
                                                     String endDate,
                                                     String idWp,
                                                     String sortBy,
                                                     String sortType,
                                                     int    pageSize,
                                                     int    pageNumber);

    /**
     * Ambil satu pengajuan top-up beserta seluruh detail kartunya.
     */
    PengajuanTopupResponse resolveById(UUID id);

    /**
     * Buat pengajuan top-up baru (header + detail).
     * Langsung update saldo_bprd di m_kartu untuk setiap kartu dalam detail.
     */
    PengajuanTopupResponse create(PengajuanTopupRequest request, String createdBy);

    /**
     * Update pengajuan top-up (header + detail — upsert/delete-not-in).
     */
    PengajuanTopupResponse update(PengajuanTopupRequest request, String updatedBy);

    /**
     * Soft-delete pengajuan top-up.
     */
    void softDelete(UUID id, String deletedBy);

    /**
     * Update status: 1 = Proses, 2 = Diapprove, 3 = Ditolak.
     */
    void updateStatus(UpdateStatusTopupRequest request);

    /**
     * Proses approval per kartu via API Bank Jatim (cek saldo VA).
     * Jika saldo VA > 0 dan status masih "1", update ke "2".
     */
    List<PengajuanTopupDetailResponse> approvalTopup(ApprovalTopupRequest request);

    /**
     * Cek saldo Virtual Account Bank Jatim (proxy ke external API).
     */
    String checkSaldoVa(CheckSaldoVaRequest request);
}
