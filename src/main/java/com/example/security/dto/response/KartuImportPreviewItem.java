package com.example.security.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Satu baris preview hasil baca Excel kartu sebelum disimpan ke DB.
 * FE menerima list ini dari POST /api/master/kartu/import,
 * lalu kirim ulang ke POST /api/master/kartu/save-import setelah konfirmasi.
 *
 * Setara dengan struct NomorImportDTO di sistem Go lama.
 */
@Data
@Builder
public class KartuImportPreviewItem {
    private String id;             // UUID kartu (opsional — kosong saat insert baru)
    private String noRfid;
    private String kodeKartu;
    private String noVa;
    private String idWp;
    private String namaWp;
    private String idKomoditas;
    private String namaKomoditas;
}
