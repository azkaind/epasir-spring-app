package com.example.security.repository;

import com.example.security.dto.response.PengajuanTopupDetailResponse;
import com.example.security.dto.response.PengajuanTopupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository untuk query native yang membutuhkan JOIN multi-tabel
 * (t_pengajuan_topup, m_wajib_pajak, t_pengajuan_topup_detail).
 *
 * Menggunakan NamedParameterJdbcTemplate agar query identik dengan
 * sistem Go lama (jelas, tidak bergantung pada Hibernate query cache).
 */
@Repository
@RequiredArgsConstructor
public class PengajuanTopupQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    // ── Base SELECT ──────────────────────────────────────────────────────
    private static final String SELECT_DTO = """
        SELECT
            pt.id,
            pt.tanggal,
            pt.nomor,
            pt.id_wp,
            pt.nominal,
            pt.file_bukti,
            pt.created_at,
            pt.created_by,
            pt.updated_at,
            pt.updated_by,
            pt.is_deleted,
            pt.status,
            pt.verified_at,
            wp.nama_wp,
            wp.iup_uop,
            wp.npwp,
            COALESCE(dt.jml_belum_topup, 0) AS jml_belum_topup,
            COALESCE(dt.jml_sudah_topup, 0) AS jml_sudah_topup,
            COALESCE(dt.jml_belum_topup, 0) + COALESCE(dt.jml_sudah_topup, 0) AS total_topup
        FROM public.t_pengajuan_topup pt
        LEFT JOIN m_wajib_pajak wp ON wp.id = pt.id_wp
        LEFT JOIN (
            SELECT
                id_pengajuan_topup,
                COALESCE(SUM(CASE WHEN status_approve_bjtm = '1' THEN 1 ELSE 0 END), 0) AS jml_belum_topup,
                COALESCE(SUM(CASE WHEN status_approve_bjtm = '2' THEN 1 ELSE 0 END), 0) AS jml_sudah_topup
            FROM public.t_pengajuan_topup_detail
            GROUP BY id_pengajuan_topup
        ) dt ON dt.id_pengajuan_topup = pt.id
        """;

    private static final String SELECT_DETAIL = """
        SELECT
            a.id,
            a.id_pengajuan_topup,
            b.kode_kartu,
            a.no_rfid,
            a.no_va,
            a.nominal,
            a.status_approve_bjtm,
            a.created_at,
            a.updated_at,
            k.nama
        FROM t_pengajuan_topup_detail a
        LEFT JOIN m_kartu b      ON b.no_rfid   = a.no_rfid
        LEFT JOIN m_komoditas k  ON k.id        = b.id_komoditas
        """;

    // ── RowMapper ────────────────────────────────────────────────────────
    private static final RowMapper<PengajuanTopupResponse> DTO_MAPPER = (rs, rowNum) ->
        PengajuanTopupResponse.builder()
            .id(rs.getString("id"))
            .tanggal(rs.getDate("tanggal") != null
                     ? rs.getDate("tanggal").toLocalDate() : null)
            .nomor(rs.getString("nomor"))
            .idWp(rs.getString("id_wp"))
            .nominal(rs.getObject("nominal") != null
                     ? rs.getDouble("nominal") : null)
            .fileBukti(rs.getString("file_bukti"))
            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
            .createdBy(rs.getString("created_by"))
            .updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
            .updatedBy(rs.getString("updated_by"))
            .isDeleted(rs.getBoolean("is_deleted"))
            .status(rs.getString("status"))
            .verifiedAt(toLocalDateTime(rs.getTimestamp("verified_at")))
            .namaWp(rs.getString("nama_wp"))
            .iupUop(rs.getString("iup_uop"))
            .npwp(rs.getString("npwp"))
            .jmlBelumTopup(rs.getInt("jml_belum_topup"))
            .jmlSudahTopup(rs.getInt("jml_sudah_topup"))
            .totalTopup(rs.getInt("total_topup"))
            .build();

    private static final RowMapper<PengajuanTopupDetailResponse> DETAIL_MAPPER = (rs, rowNum) ->
        PengajuanTopupDetailResponse.builder()
            .id(rs.getString("id"))
            .idPengajuanTopup(rs.getString("id_pengajuan_topup"))
            .kodeKartu(rs.getString("kode_kartu"))
            .noRfid(rs.getString("no_rfid"))
            .noVa(rs.getString("no_va"))
            .nominal(rs.getObject("nominal") != null ? rs.getDouble("nominal") : null)
            .statusApproveBjtm(rs.getString("status_approve_bjtm"))
            .nama(rs.getString("nama"))
            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
            .updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
            .build();

    // ── Public methods ────────────────────────────────────────────────────

    /**
     * Hitung total data + total rupiah untuk pagination meta.
     */
    public long countAll(String keyword, String startDate, String endDate, String idWp) {
        String sql = "SELECT COUNT(x.id) FROM (" + SELECT_DTO
                + buildWhere(keyword, startDate, endDate, idWp) + ") x";
        MapSqlParameterSource params = buildParams(keyword, startDate, endDate, idWp);
        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }

    /**
     * Hitung total nominal untuk pagination meta.
     */
    public Double sumNominal(String keyword, String startDate, String endDate, String idWp) {
        String sql = "SELECT COALESCE(SUM(x.nominal), 0) FROM (" + SELECT_DTO
                + buildWhere(keyword, startDate, endDate, idWp) + ") x";
        MapSqlParameterSource params = buildParams(keyword, startDate, endDate, idWp);
        return jdbc.queryForObject(sql, params, Double.class);
    }

    /**
     * List dengan filter, sorting, dan pagination.
     */
    public List<PengajuanTopupResponse> findAll(String keyword,
                                                String startDate,
                                                String endDate,
                                                String idWp,
                                                String sortBy,
                                                String sortType,
                                                int pageSize,
                                                int pageNumber) {
        String orderCol = resolveOrderColumn(sortBy);
        String order    = "DESC".equalsIgnoreCase(sortType) ? "DESC" : "ASC";
        int    offset   = (pageNumber - 1) * pageSize;

        String sql = SELECT_DTO
                + buildWhere(keyword, startDate, endDate, idWp)
                + " ORDER BY " + orderCol + " " + order
                + " LIMIT :limit OFFSET :offset";

        MapSqlParameterSource params = buildParams(keyword, startDate, endDate, idWp)
                .addValue("limit",  pageSize)
                .addValue("offset", offset);

        return jdbc.query(sql, params, DTO_MAPPER);
    }

    /**
     * Ambil satu record dengan join (untuk ResolveByID).
     */
    public PengajuanTopupResponse findDtoById(String id) {
        String sql = SELECT_DTO + " WHERE pt.id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        List<PengajuanTopupResponse> result = jdbc.query(sql, params, DTO_MAPPER);
        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * Ambil semua detail untuk satu pengajuan, urut kode_kartu asc.
     */
    public List<PengajuanTopupDetailResponse> findDetailByTopupId(String idPengajuanTopup) {
        String sql = SELECT_DETAIL
                + " WHERE a.id_pengajuan_topup = :idTopup ORDER BY b.kode_kartu ASC";
        MapSqlParameterSource params = new MapSqlParameterSource("idTopup", idPengajuanTopup);
        return jdbc.query(sql, params, DETAIL_MAPPER);
    }

    /**
     * Ambil satu detail berdasarkan id (untuk proses approval per kartu).
     */
    public PengajuanTopupDetailResponse findDetailById(String id) {
        String sql = SELECT_DETAIL + " WHERE a.id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        List<PengajuanTopupDetailResponse> result = jdbc.query(sql, params, DETAIL_MAPPER);
        return result.isEmpty() ? null : result.get(0);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private String buildWhere(String keyword, String startDate, String endDate, String idWp) {
        StringBuilder sb = new StringBuilder(" WHERE COALESCE(pt.is_deleted, false) = false ");

        if (keyword != null && !keyword.isBlank()) {
            sb.append(" AND CONCAT(wp.nama_wp, pt.nomor, pt.nominal) ILIKE :keyword ");
        }
        if (startDate != null && !startDate.isBlank()
                && endDate != null && !endDate.isBlank()) {
            sb.append(" AND pt.tanggal >= CAST(:startDate AS date) AND pt.tanggal <= CAST(:endDate AS date) ");
        }
        if (idWp != null && !idWp.isBlank()) {
            sb.append(" AND pt.id_wp = :idWp ");
        }
        return sb.toString();
    }

    private MapSqlParameterSource buildParams(String keyword, String startDate,
                                              String endDate, String idWp) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        if (keyword != null && !keyword.isBlank()) {
            p.addValue("keyword", "%" + keyword + "%");
        }
        if (startDate != null && !startDate.isBlank()) p.addValue("startDate", startDate);
        if (endDate   != null && !endDate.isBlank())   p.addValue("endDate",   endDate);
        if (idWp      != null && !idWp.isBlank())      p.addValue("idWp",      idWp);
        return p;
    }

    private String resolveOrderColumn(String sortBy) {
        return switch (sortBy == null ? "" : sortBy) {
            case "tanggal"   -> "pt.tanggal";
            case "nominal"   -> "pt.nominal";
            case "nomor"     -> "pt.nomor";
            case "idWp"      -> "pt.id_wp";
            case "namaWp"    -> "wp.nama_wp";
            case "updatedAt" -> "pt.updated_at";
            default          -> "pt.created_at";
        };
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
