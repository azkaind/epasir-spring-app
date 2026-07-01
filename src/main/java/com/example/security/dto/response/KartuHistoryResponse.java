package com.example.security.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response gabungan: detail kartu + list riwayat transaksi.
 * Setara dengan struct HistoryKartu di sistem Go lama.
 *
 * GET /api/master/kartu/history/{kodeKartu}
 */
@Data
@Builder
public class KartuHistoryResponse {
    private KartuDetailResponse      kartu;
    private List<KartuHistoryItem>   history;
}
