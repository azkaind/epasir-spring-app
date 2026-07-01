package com.example.security.repository;

import com.example.security.dto.response.KartuDetailResponse;
import com.example.security.dto.response.KartuListResponse;
import com.example.security.dto.response.KartuHistoryItem;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Native query repository untuk Kartu — mengikuti pola PengajuanTopupQueryRepository.
 *
 * JOIN utama (setara SelectDTO di Go):
 *   m_kartu k
 *   LEFT JOIN m_komoditas mk ON k.id_komoditas = mk.id
 *   LEFT JOIN t_pengajuan_topup_detail td ON td.no_va = k.no_va
 *   LEFT JOIN m_wajib_pajak wp ON wp.id = k.id_wp
 *
 * terakhir_topup = MAX(td.created_at)
 */
@Repository
@RequiredArgsConstructor
public class KartuQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    // ── SELECT LIST ──────────────────────────────────────────────────────────
    private static final String SELECT_LIST = """
        SELECT k.id, k.no_rfid, k.kode_kartu, k.no_va, k.saldo, k.saldo_bprd,
               k.id_wp, k.nama_wp, k.id_komoditas,
               mk.nama AS nama_komoditas, mk.nominal,
               k.created_at, k.updated_at, k.is_deleted,
               COALESCE(MAX(td.created_at), '2023-08-15 13:48:01'::timestamp) AS terakhir_topup
        FROM m_kartu k
        LEFT JOIN m_komoditas mk ON k.id_komoditas = mk.id
        LEFT JOIN t_pengajuan_topup_detail td ON td.no_va = k.no_va
        LEFT JOIN m_wajib_pajak wp ON wp.id = k.id_wp
        """;

    // ── SELECT DETAIL ────────────────────────────────────────────────────────
    private static final String SELECT_DETAIL = """
        SELECT k.id, k.no_rfid, k.kode_kartu, k.no_va, k.saldo, k.saldo_bprd,
               k.id_wp, k.nama_wp, k.id_komoditas,
               mk.nama AS nama_komoditas, mk.nominal,
               k.armada, k.nopol, k.tujuan, k.keterangan, k.aktif,
               k.tgl_pendaftaran, k.porporasi, k.last_topup,
               k.created_at, k.created_by, k.updated_at, k.updated_by, k.is_deleted,
               wp.kode_bprd, wp.kode_bjtm,
               COALESCE(MAX(td.created_at), '2023-08-15 13:48:01'::timestamp) AS terakhir_topup
        FROM m_kartu k
        LEFT JOIN m_komoditas mk ON k.id_komoditas = mk.id
        LEFT JOIN t_pengajuan_topup_detail td ON td.no_va = k.no_va
        LEFT JOIN m_wajib_pajak wp ON wp.id = k.id_wp
        """;

    // ── SELECT HISTORY (riwayat topup per kartu — minimal version) ──────────
    private static final String SELECT_HISTORY = """
        SELECT d.created_at AS time_taping,
               k.no_rfid, k.no_va, k.kode_kartu,
               d.nominal, '' AS trx_no, '' AS gate_taping,
               wp.nama_wp, 'TOPUP' AS jenis_trx
        FROM t_pengajuan_topup_detail d
        LEFT JOIN m_kartu k ON k.no_rfid = d.no_rfid
        LEFT JOIN t_pengajuan_topup pt ON pt.id::text = d.id_pengajuan_topup
        LEFT JOIN m_wajib_pajak wp ON wp.id = pt.id_wp
        WHERE k.kode_kartu = :kodeKartu
          AND d.status_approve_bjtm = '2'
        ORDER BY d.created_at DESC
        """;

    private static final RowMapper<KartuListResponse> LIST_MAPPER = (rs, i) ->
        KartuListResponse.builder()
            .id(rs.getString("id"))
            .noRfid(rs.getString("no_rfid"))
            .kodeKartu(rs.getString("kode_kartu"))
            .noVa(rs.getString("no_va"))
            .namaWp(rs.getString("nama_wp"))
            .namaKomoditas(rs.getString("nama_komoditas"))
            .nominal(rs.getObject("nominal") != null ? rs.getInt("nominal") : null)
            .saldo(rs.getObject("saldo") != null ? rs.getDouble("saldo") : null)
            .saldoBprd(rs.getObject("saldo_bprd") != null ? rs.getDouble("saldo_bprd") : null)
            .terakhirTopup(toLocalDateTime(rs.getTimestamp("terakhir_topup")))
            .build();

    private static final RowMapper<KartuHistoryItem> HISTORY_MAPPER = (rs, i) ->
        KartuHistoryItem.builder()
            .timeTaping(toLocalDateTime(rs.getTimestamp("time_taping")))
            .noRfid(rs.getString("no_rfid"))
            .noVa(rs.getString("no_va"))
            .kodeKartu(rs.getString("kode_kartu"))
            .nominal(rs.getDouble("nominal"))
            .trxNo(rs.getString("trx_no"))
            .gateTaping(rs.getString("gate_taping"))
            .namaWp(rs.getString("nama_wp"))
            .jenisTrx(rs.getString("jenis_trx"))
            .build();

    // ── Public methods ────────────────────────────────────────────────────────

    public long count(String keyword, String idWp) {
        String sql = "SELECT COUNT(*) FROM (" + SELECT_LIST + buildWhere(keyword, idWp)
                   + " GROUP BY k.id, mk.nama, mk.nominal) x";
        Long c = jdbc.queryForObject(sql, buildParams(keyword, idWp), Long.class);
        return c != null ? c : 0L;
    }

    public List<KartuListResponse> findAll(String keyword, String idWp,
                                            String sortBy, String sortType,
                                            int pageSize, int pageNumber) {
        String col    = resolveCol(sortBy);
        String order  = "DESC".equalsIgnoreCase(sortType) ? "DESC" : "ASC";
        int    offset = (pageNumber - 1) * pageSize;
        String sql    = SELECT_LIST + buildWhere(keyword, idWp)
                      + " GROUP BY k.id, mk.nama, mk.nominal"
                      + " ORDER BY " + col + " " + order
                      + " LIMIT :limit OFFSET :offset";
        MapSqlParameterSource p = buildParams(keyword, idWp)
                .addValue("limit", pageSize).addValue("offset", offset);
        return jdbc.query(sql, p, LIST_MAPPER);
    }

    public KartuListResponse findDetailById(String id) {
        String sql = SELECT_LIST + " WHERE k.id = :id AND COALESCE(k.is_deleted,false)=false"
                   + " GROUP BY k.id, mk.nama, mk.nominal";
        List<KartuListResponse> r = jdbc.query(sql, new MapSqlParameterSource("id", id), LIST_MAPPER);
        return r.isEmpty() ? null : r.get(0);
    }

    /** Lookup by kode_kartu OR no_rfid OR no_va (setara ResolveByAndro di sistem lama) */
    public List<KartuDetailResponse> findByKodeOrRfid(String nomor) {
        String sql = SELECT_DETAIL
                   + " WHERE (k.kode_kartu = :nomor OR k.no_rfid = :nomor OR k.no_va = :nomor)"
                   + "   AND COALESCE(k.is_deleted, false) = false"
                   + " GROUP BY k.id, mk.nama, mk.nominal, wp.kode_bprd, wp.kode_bjtm";
        RowMapper<KartuDetailResponse> mapper = buildDetailMapper();
        return jdbc.query(sql, new MapSqlParameterSource("nomor", nomor), mapper);
    }

    public List<KartuHistoryItem> findHistory(String kodeKartu) {
        return jdbc.query(SELECT_HISTORY, new MapSqlParameterSource("kodeKartu", kodeKartu), HISTORY_MAPPER);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String buildWhere(String keyword, String idWp) {
        StringBuilder sb = new StringBuilder(" WHERE COALESCE(k.is_deleted, false) = false ");
        if (keyword != null && !keyword.isBlank())
            sb.append(" AND CONCAT(k.no_rfid, k.kode_kartu, k.no_va, k.nama_wp, mk.nama) ILIKE :keyword ");
        if (idWp != null && !idWp.isBlank())
            sb.append(" AND k.id_wp = :idWp ");
        return sb.toString();
    }

    private MapSqlParameterSource buildParams(String keyword, String idWp) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        if (keyword != null && !keyword.isBlank()) p.addValue("keyword", "%" + keyword + "%");
        if (idWp    != null && !idWp.isBlank())    p.addValue("idWp",    idWp);
        return p;
    }

    private String resolveCol(String sortBy) {
        return switch (sortBy == null ? "" : sortBy) {
            case "kodeKartu"    -> "k.kode_kartu";
            case "noRfid"       -> "k.no_rfid";
            case "namaWp"       -> "k.nama_wp";
            case "saldo"        -> "k.saldo";
            case "terakhirTopup"-> "terakhir_topup";
            case "updatedAt"    -> "k.updated_at";
            default             -> "k.created_at";
        };
    }

    private RowMapper<KartuDetailResponse> buildDetailMapper() {
        return (rs, i) -> KartuDetailResponse.builder()
            .id(rs.getString("id"))
            .noRfid(rs.getString("no_rfid"))
            .kodeKartu(rs.getString("kode_kartu"))
            .noVa(rs.getString("no_va"))
            .saldo(rs.getObject("saldo") != null ? rs.getDouble("saldo") : null)
            .saldoBprd(rs.getObject("saldo_bprd") != null ? rs.getDouble("saldo_bprd") : null)
            .namaWp(rs.getString("nama_wp"))
            .idWp(rs.getString("id_wp"))
            .idKomoditas(rs.getString("id_komoditas"))
            .namaKomoditas(rs.getString("nama_komoditas"))
            .nominal(rs.getObject("nominal") != null ? rs.getInt("nominal") : null)
            .armada(rs.getString("armada"))
            .nopol(rs.getString("nopol"))
            .tujuan(rs.getString("tujuan"))
            .keterangan(rs.getString("keterangan"))
            .kodeBprd(rs.getString("kode_bprd"))
            .kodeBjtm(rs.getString("kode_bjtm"))
            .terakhirTopup(toLocalDateTime(rs.getTimestamp("terakhir_topup")))
            .build();
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
