package com.example.security.service.impl;

import com.example.security.dto.request.KartuRequest;
import com.example.security.dto.response.KartuDetailResponse;
import com.example.security.dto.response.KartuDropdownResponse;
import com.example.security.dto.response.KartuHistoryItem;
import com.example.security.dto.response.KartuHistoryResponse;
import com.example.security.dto.response.KartuImportPreviewItem;
import com.example.security.dto.response.KartuListResponse;
import com.example.security.dto.response.PagedResponse;
import com.example.security.entity.Kartu;
import com.example.security.exception.BadRequestException;
import com.example.security.exception.UserNotFoundException;
import com.example.security.repository.KartuQueryRepository;
import com.example.security.repository.KartuRepository;
import com.example.security.repository.KomoditasRepository;
import com.example.security.repository.WajibPajakRepository;
import com.example.security.service.KartuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementasi KartuService (Master Kartu/RFID).
 *
 * Setara dengan NomorServiceImpl di sistem Go lama.
 * Mengikuti pola yang sudah ada:
 *  - KartuRepository  (JPA) untuk CRUD sederhana
 *  - KartuQueryRepository (JDBC) untuk list/query JOIN kompleks
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class KartuServiceImpl implements KartuService {

    private final KartuRepository       kartuRepository;
    private final KartuQueryRepository  kartuQueryRepository;
    private final WajibPajakRepository  wpRepository;
    private final KomoditasRepository   komoditasRepository;

    // ── LIST & SEARCH ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<KartuListResponse> resolveAll(String q, String idWp,
                                                        int pageSize, int pageNumber,
                                                        String sortBy, String sortType) {
        long total = kartuQueryRepository.count(q, idWp);
        if (total == 0) return PagedResponse.of(List.of(), pageNumber, pageSize, 0L, null);
        List<KartuListResponse> items = kartuQueryRepository.findAll(q, idWp, sortBy, sortType, pageSize, pageNumber);
        return PagedResponse.of(items, pageNumber, pageSize, total, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KartuDropdownResponse> getAll() {
        return kartuRepository.findAllByIsDeletedFalseOrderByKodeKartuAsc()
                .stream()
                .map(k -> KartuDropdownResponse.builder()
                        .id(k.getId().toString())
                        .kodeKartu(k.getKodeKartu())
                        .noRfid(k.getNoRfid())
                        .namaWp(k.getNamaWp())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public KartuListResponse resolveById(UUID id) {
        KartuListResponse dto = kartuQueryRepository.findDetailById(id.toString());
        if (dto == null)
            throw new UserNotFoundException("Kartu dengan ID: " + id + " tidak ditemukan");
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KartuDetailResponse> resolveByKodeOrRfid(String nomor) {
        return kartuQueryRepository.findByKodeOrRfid(nomor);
    }

    // ── HISTORY ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public KartuHistoryResponse getHistory(String kodeKartu) {
        // Ambil detail kartu terlebih dahulu
        List<KartuDetailResponse> kartuList = kartuQueryRepository.findByKodeOrRfid(kodeKartu);
        KartuDetailResponse kartu = kartuList.isEmpty() ? null : kartuList.get(0);

        // Ambil riwayat topup yang sudah diapprove
        List<KartuHistoryItem> history = kartuQueryRepository.findHistory(kodeKartu);

        return KartuHistoryResponse.builder()
                .kartu(kartu)
                .history(history)
                .build();
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Override
    public KartuListResponse create(KartuRequest req, String userId) {
        // Validasi duplikat kode kartu
        if (kartuRepository.existsByKodeKartuIgnoreCaseAndIsDeletedFalse(req.getKodeKartu()))
            throw new BadRequestException("Kode kartu '" + req.getKodeKartu() + "' sudah terdaftar");

        // Resolve namaWp dari idWp jika namaWp tidak diisi
        String namaWp = req.getNamaWp();
        if ((namaWp == null || namaWp.isBlank()) && req.getIdWp() != null && !req.getIdWp().isBlank()) {
            namaWp = wpRepository.findById(UUID.fromString(req.getIdWp()))
                    .map(wp -> wp.getNamaWp())
                    .orElse(null);
        }

        Kartu entity = Kartu.builder()
                .noRfid(req.getNoRfid())
                .kodeKartu(req.getKodeKartu())
                .noVa(req.getNoVa())
                .namaWp(namaWp)
                .idWp(req.getIdWp())
                .idKomoditas(req.getIdKomoditas())
                .armada(req.getArmada())
                .nopol(req.getNopol())
                .tujuan(req.getTujuan())
                .keterangan(req.getKeterangan())
                .aktif(req.getAktif() != null ? req.getAktif() : true)
                .tglPendaftaran(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .createdBy(userId)
                .isDeleted(false)
                .build();

        Kartu saved = kartuRepository.save(entity);
        return resolveById(saved.getId());
    }

    @Override
    public KartuListResponse update(UUID id, KartuRequest req, String userId) {
        Kartu existing = kartuRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Kartu dengan ID: " + id + " tidak ditemukan"));

        // Resolve namaWp jika idWp berubah
        String namaWp = req.getNamaWp();
        if ((namaWp == null || namaWp.isBlank()) && req.getIdWp() != null && !req.getIdWp().isBlank()) {
            namaWp = wpRepository.findById(UUID.fromString(req.getIdWp()))
                    .map(wp -> wp.getNamaWp())
                    .orElse(null);
        }

        existing.setNoRfid(req.getNoRfid());
        existing.setKodeKartu(req.getKodeKartu());
        existing.setNoVa(req.getNoVa());
        existing.setNamaWp(namaWp);
        existing.setIdWp(req.getIdWp());
        existing.setIdKomoditas(req.getIdKomoditas());
        existing.setArmada(req.getArmada());
        existing.setNopol(req.getNopol());
        existing.setTujuan(req.getTujuan());
        existing.setKeterangan(req.getKeterangan());
        if (req.getAktif() != null) existing.setAktif(req.getAktif());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(userId);

        kartuRepository.save(existing);
        return resolveById(id);
    }

    @Override
    public void softDelete(UUID id, String userId) {
        Kartu existing = kartuRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Kartu dengan ID: " + id + " tidak ditemukan"));
        existing.setIsDeleted(true);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(userId);
        kartuRepository.save(existing);
    }

    // ── EXCEL IMPORT ──────────────────────────────────────────────────────────

    /**
     * Baca file Excel kartu, kembalikan preview tanpa INSERT ke DB.
     *
     * Kolom Excel (baris 1 = header, data mulai baris 2):
     *   A=No | B=No RFID | C=Kode Kartu | D=No VA | E=Nama WP | F=Nama Komoditas
     *
     * Setara ImportKartu (preview) di sistem Go lama.
     */
    @Override
    @Transactional(readOnly = true)
    public List<KartuImportPreviewItem> previewImport(MultipartFile file) {
        List<KartuImportPreviewItem> result = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            // Mulai dari baris 1 (skip header baris 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String noRfid        = getCellString(row, 1); // B
                String kodeKartu     = getCellString(row, 2); // C
                String noVa          = getCellString(row, 3); // D
                String namaWpRaw     = getCellString(row, 4); // E
                String namaKomoditas = getCellString(row, 5); // F

                if (noRfid.isBlank() && kodeKartu.isBlank()) continue; // skip baris kosong

                // Resolve UUID Wajib Pajak by nama
                String idWp   = null;
                String namaWp = namaWpRaw;
                if (!namaWpRaw.isBlank()) {
                    var wpOpt = wpRepository.findByNamaWpIgnoreCaseAndIsDeletedFalse(namaWpRaw);
                    if (wpOpt.isPresent()) {
                        idWp   = wpOpt.get().getId().toString();
                        namaWp = wpOpt.get().getNamaWp();
                    }
                }

                // Resolve UUID Komoditas by nama
                String idKomoditas = null;
                if (!namaKomoditas.isBlank()) {
                    var komOpt = komoditasRepository.findByNamaIgnoreCaseAndIsDeletedFalse(namaKomoditas);
                    if (komOpt.isPresent()) {
                        idKomoditas = komOpt.get().getId().toString();
                    }
                }

                result.add(KartuImportPreviewItem.builder()
                        .noRfid(noRfid)
                        .kodeKartu(kodeKartu)
                        .noVa(noVa)
                        .idWp(idWp)
                        .namaWp(namaWp)
                        .idKomoditas(idKomoditas)
                        .namaKomoditas(namaKomoditas)
                        .build());
            }
        } catch (IOException e) {
            throw new BadRequestException("Gagal membaca file Excel: " + e.getMessage());
        }
        return result;
    }

    /**
     * Simpan hasil preview import Excel ke DB.
     * Dipanggil setelah user konfirmasi data di FE.
     *
     * Setara SaveImport di sistem Go lama.
     */
    @Override
    public void saveImport(List<KartuImportPreviewItem> data, String userId) {
        for (KartuImportPreviewItem item : data) {
            // Cek apakah sudah ada — update jika ada, insert jika tidak
            var existing = kartuRepository.findByKodeKartuAndIsDeletedFalse(item.getKodeKartu());
            if (existing.isPresent()) {
                Kartu k = existing.get();
                k.setNoRfid(item.getNoRfid());
                k.setNoVa(item.getNoVa());
                k.setNamaWp(item.getNamaWp());
                k.setIdWp(item.getIdWp());
                k.setIdKomoditas(item.getIdKomoditas());
                k.setUpdatedAt(LocalDateTime.now());
                k.setUpdatedBy(userId);
                kartuRepository.save(k);
            } else {
                String namaWp = item.getNamaWp();
                if ((namaWp == null || namaWp.isBlank()) && item.getIdWp() != null) {
                    namaWp = wpRepository.findById(UUID.fromString(item.getIdWp()))
                            .map(wp -> wp.getNamaWp()).orElse(null);
                }
                Kartu k = Kartu.builder()
                        .noRfid(item.getNoRfid())
                        .kodeKartu(item.getKodeKartu())
                        .noVa(item.getNoVa())
                        .namaWp(namaWp)
                        .idWp(item.getIdWp())
                        .idKomoditas(item.getIdKomoditas())
                        .tglPendaftaran(LocalDateTime.now())
                        .createdAt(LocalDateTime.now())
                        .createdBy(userId)
                        .isDeleted(false)
                        .build();
                kartuRepository.save(k);
            }
        }
    }

    /**
     * Import Top Up Massal — baca Excel, cocokkan kartu dengan idWp.
     * Hanya mengembalikan list kartu yang cocok; TIDAK membuat PengajuanTopup.
     * FE yang submit ke POST /api/bprd/pengajuan-topup setelah konfirmasi.
     *
     * Kolom Excel: B=No RFID | C=Kode Kartu | D=No VA
     */
    @Override
    @Transactional(readOnly = true)
    public List<KartuListResponse> importTopup(MultipartFile file, String idWp) {
        List<String> noVaList = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String noVa = getCellString(row, 3); // kolom D
                if (!noVa.isBlank()) noVaList.add(noVa);
            }
        } catch (IOException e) {
            throw new BadRequestException("Gagal membaca file Excel topup: " + e.getMessage());
        }

        if (noVaList.isEmpty()) return List.of();

        // Filter kartu yang no_va-nya ada di list DAN sesuai idWp
        return noVaList.stream()
                .map(noVa -> kartuRepository.findByNoVaAndIsDeletedFalse(noVa).orElse(null))
                .filter(k -> k != null && (idWp == null || idWp.isBlank() || idWp.equals(k.getIdWp())))
                .map(k -> kartuQueryRepository.findDetailById(k.getId().toString()))
                .filter(dto -> dto != null)
                .toList();
    }

    /**
     * Download template Excel kosong untuk import kartu.
     * File tersimpan di src/main/resources/templates/template_import_kartu.xlsx
     */
    @Override
    @Transactional(readOnly = true)
    public Resource downloadTemplate() {
        Resource resource = new ClassPathResource("templates/template_import_kartu.xlsx");
        if (!resource.exists())
            throw new BadRequestException("File template tidak ditemukan. Hubungi administrator.");
        return resource;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String getCellString(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                // Cegah "123.0" — format sebagai integer jika tidak ada desimal
                yield val == Math.floor(val) ? String.valueOf((long) val) : String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> "";
        };
    }
}
