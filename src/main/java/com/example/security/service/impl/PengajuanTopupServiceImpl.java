package com.example.security.service.impl;

import com.example.security.dto.request.ApprovalTopupRequest;
import com.example.security.dto.request.CheckSaldoVaRequest;
import com.example.security.dto.request.PengajuanTopupRequest;
import com.example.security.dto.request.UpdateStatusTopupRequest;
import com.example.security.dto.response.PagedResponse;
import com.example.security.dto.response.PengajuanTopupDetailResponse;
import com.example.security.dto.response.PengajuanTopupResponse;
import com.example.security.entity.PengajuanTopup;
import com.example.security.entity.PengajuanTopupDetail;
import com.example.security.exception.BadRequestException;
import com.example.security.exception.UserNotFoundException;
import com.example.security.external.BankJatimClient;
import com.example.security.repository.PengajuanTopupDetailRepository;
import com.example.security.repository.PengajuanTopupQueryRepository;
import com.example.security.repository.PengajuanTopupRepository;
import com.example.security.service.PengajuanTopupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PengajuanTopupServiceImpl implements PengajuanTopupService {

    private final PengajuanTopupRepository       topupRepository;
    private final PengajuanTopupDetailRepository detailRepository;
    private final PengajuanTopupQueryRepository  queryRepository;
    private final BankJatimClient                bankJatimClient;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── resolveAll ────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PengajuanTopupResponse> resolveAll(String keyword,
                                                            String startDate,
                                                            String endDate,
                                                            String idWp,
                                                            String sortBy,
                                                            String sortType,
                                                            int    pageSize,
                                                            int    pageNumber) {
        long   totalData   = queryRepository.countAll(keyword, startDate, endDate, idWp);
        Double totalRupiah = queryRepository.sumNominal(keyword, startDate, endDate, idWp);

        if (totalData == 0) {
            return PagedResponse.of(List.of(), pageNumber, pageSize, 0L, 0.0);
        }

        List<PengajuanTopupResponse> items = queryRepository.findAll(
                keyword, startDate, endDate, idWp,
                sortBy, sortType, pageSize, pageNumber);

        return PagedResponse.of(items, pageNumber, pageSize, totalData, totalRupiah);
    }

    // ── resolveById ───────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public PengajuanTopupResponse resolveById(UUID id) {
        PengajuanTopupResponse dto = queryRepository.findDtoById(id.toString());
        if (dto == null) {
            throw new UserNotFoundException(
                    "Pengajuan top-up dengan ID: " + id + " tidak ditemukan");
        }

        List<PengajuanTopupDetailResponse> details =
                queryRepository.findDetailByTopupId(id.toString());
        dto.setDetail(details);
        return dto;
    }

    // ── create ────────────────────────────────────────────────────────────
    @Override
    public PengajuanTopupResponse create(PengajuanTopupRequest request, String createdBy) {
        String status = (request.getStatus() != null && !request.getStatus().isBlank())
                        ? request.getStatus()
                        : "1";

        LocalDate tanggal = (request.getTanggal() != null && !request.getTanggal().isBlank())
                            ? LocalDate.parse(request.getTanggal(), DATE_FMT)
                            : LocalDate.now();

        // ── Build header ─────────────────────────────────────────────────
        PengajuanTopup topup = PengajuanTopup.builder()
                .tanggal(tanggal)
                .idWp(request.getIdWp())
                .nominal(request.getNominal())
                .fileBukti(request.getFileBukti())
                .status(status)
                .createdAt(LocalDateTime.now())
                .createdBy(createdBy)
                .isDeleted(false)
                .build();

        // ── Build details ─────────────────────────────────────────────────
        List<PengajuanTopupDetail> details = buildDetails(
                request.getDetail(), topup.getId() /* null saat build, diisi setelah save */);

        // Simpan header dulu agar ID-nya di-generate DB/Hibernate
        PengajuanTopup saved = topupRepository.save(topup);

        // Isi id_pengajuan_topup setelah header tersimpan
        details.forEach(d -> d.setIdPengajuanTopup(saved.getId().toString()));
        detailRepository.saveAll(details);

        // ── Update saldo_bprd di m_kartu (sama seperti service.go) ───────
        updateSaldoBprdBulk(request.getDetail(), createdBy);

        return resolveById(saved.getId());
    }

    // ── update ────────────────────────────────────────────────────────────
    @Override
    public PengajuanTopupResponse update(PengajuanTopupRequest request, String updatedBy) {
        UUID topupId = parseUUID(request.getId(),
                "ID pengajuan top-up tidak valid: " + request.getId());

        PengajuanTopup existing = topupRepository.findById(topupId)
                .orElseThrow(() -> new UserNotFoundException(
                        "Data pengajuan top-up dengan ID: " + request.getId() + " tidak ditemukan"));

        // Pertahankan status lama (hanya boleh diubah via updateStatus)
        LocalDate tanggal = (request.getTanggal() != null && !request.getTanggal().isBlank())
                            ? LocalDate.parse(request.getTanggal(), DATE_FMT)
                            : existing.getTanggal();

        existing.setTanggal(tanggal);
        existing.setIdWp(request.getIdWp());
        existing.setNominal(request.getNominal());
        existing.setFileBukti(request.getFileBukti());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        topupRepository.save(existing);

        // ── Update detail: hapus yang tidak ada, upsert sisanya ──────────
        List<String> incomingIds = request.getDetail().stream()
                .map(d -> d.getId() != null ? d.getId() : "")
                .filter(id -> !id.isBlank())
                .collect(Collectors.toList());

        if (!incomingIds.isEmpty()) {
            detailRepository.deleteNotIn(topupId.toString(), incomingIds);
        } else {
            // Semua detail lama dihapus, ganti semua baru
            detailRepository.deleteByIdPengajuanTopup(topupId.toString());
        }

        List<PengajuanTopupDetail> newDetails = buildDetails(
                request.getDetail(), topupId);
        newDetails.forEach(d -> d.setIdPengajuanTopup(topupId.toString()));
        detailRepository.saveAll(newDetails);

        return resolveById(topupId);
    }

    // ── softDelete ────────────────────────────────────────────────────────
    @Override
    public void softDelete(UUID id, String deletedBy) {
        if (!topupRepository.existsById(id)) {
            throw new UserNotFoundException(
                    "Data pengajuan top-up dengan ID: " + id + " tidak ditemukan");
        }
        topupRepository.softDelete(id, LocalDateTime.now(), deletedBy);
    }

    // ── updateStatus ──────────────────────────────────────────────────────
    @Override
    public void updateStatus(UpdateStatusTopupRequest request) {
        UUID topupId = parseUUID(request.getId(),
                "ID pengajuan top-up tidak valid: " + request.getId());

        if (!topupRepository.existsById(topupId)) {
            throw new UserNotFoundException(
                    "Data pengajuan top-up dengan ID: " + request.getId() + " tidak ditemukan");
        }

        LocalDateTime now = LocalDateTime.now();
        topupRepository.updateStatus(topupId, request.getStatus(), now, now);
    }

    // ── approvalTopup ─────────────────────────────────────────────────────
    @Override
    public List<PengajuanTopupDetailResponse> approvalTopup(ApprovalTopupRequest request) {
        List<PengajuanTopupDetailResponse> result = new ArrayList<>();

        for (ApprovalTopupRequest.ApprovalTopupDetailRequest item : request.getData()) {
            // Cari detail
            PengajuanTopupDetailResponse detailDto =
                    queryRepository.findDetailById(item.getId());

            boolean found = detailDto != null;

            // Panggil API Bank Jatim untuk cek saldo VA
            String rawResp  = bankJatimClient.cekSaldoVA(item.getNoVa());
            boolean apiOk   = rawResp != null;
            double  nominal = apiOk ? bankJatimClient.parseNominal(rawResp) : 0.0;

            // Update ke "2" (sudah ditopup) jika kondisi terpenuhi
            if (apiOk && found && nominal > 0
                    && "1".equals(detailDto.getStatusApproveBjtm())) {
                LocalDateTime now = LocalDateTime.now();
                UUID detailId = UUID.fromString(item.getId());
                detailRepository.updateStatusApproveBjtm(detailId, "2", now);

                // Refresh DTO agar statusApproveBjtm tercermin
                detailDto = queryRepository.findDetailById(item.getId());
            }

            if (detailDto != null) {
                result.add(detailDto);
            }
        }

        return result;
    }

    // ── checkSaldoVa ──────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public String checkSaldoVa(CheckSaldoVaRequest request) {
        String raw = bankJatimClient.cekSaldoVA(request.getVirtualAccount());
        return raw != null ? raw : "{\"error\":\"Gagal menghubungi Bank Jatim\"}";
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private List<PengajuanTopupDetail> buildDetails(
            List<com.example.security.dto.request.PengajuanTopupDetailRequest> requests,
            UUID parentId) {

        List<PengajuanTopupDetail> details = new ArrayList<>();
        for (var req : requests) {
            String status = (req.getStatusApproveBjtm() != null
                             && !req.getStatusApproveBjtm().isBlank())
                            ? req.getStatusApproveBjtm()
                            : "1";

            PengajuanTopupDetail d = PengajuanTopupDetail.builder()
                    .noRfid(req.getNoRfid())
                    .noVa(req.getNoVa())
                    .nominal(req.getNominal())
                    .statusApproveBjtm(status)
                    .createdAt(LocalDateTime.now())
                    .build();

            // Jika detail sudah ada (update), pertahankan ID-nya
            if (req.getId() != null && !req.getId().isBlank()) {
                d.setId(UUID.fromString(req.getId()));
                d.setCreatedAt(null); // biarkan DB mempertahankan nilai lama
                d.setUpdatedAt(LocalDateTime.now());
            }

            if (parentId != null) {
                d.setIdPengajuanTopup(parentId.toString());
            }

            details.add(d);
        }
        return details;
    }

    private void updateSaldoBprdBulk(
            List<com.example.security.dto.request.PengajuanTopupDetailRequest> details,
            String updatedBy) {
        LocalDateTime now = LocalDateTime.now();
        for (var d : details) {
            try {
                detailRepository.updateSaldoBprd(d.getNoRfid(), d.getNominal(), now, updatedBy);
            } catch (Exception e) {
                log.error("Gagal update saldo_bprd untuk noRfid={}: {}",
                          d.getNoRfid(), e.getMessage());
                // Tidak melempar exception agar satu kegagalan tidak
                // rollback seluruh transaksi — konsisten dengan perilaku sistem Go.
            }
        }
    }

    private UUID parseUUID(String value, String errorMsg) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(errorMsg);
        }
    }
}
