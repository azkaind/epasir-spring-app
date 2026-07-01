package com.example.security.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Satu baris riwayat transaksi kartu.
 * Setara dengan struct NomorHistory di sistem Go lama.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KartuHistoryItem {
    private LocalDateTime timeTaping;
    private String        noRfid;
    private String        noVa;
    private String        kodeKartu;
    private Double        nominal;
    private String        trxNo;
    private String        gateTaping;
    private String        namaWp;
    private String        jenisTrx;
}
