package com.example.security.repository;

import com.example.security.dto.response.WajibPajakResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class WajibPajakQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String SELECT_BASE = """
        SELECT id, kode_bprd, kode_bjtm, nama_wp, desa, kecamatan, iup_uop, komoditas,
               ijin_berlaku, npwp, status_perizinan, no_customer,
               created_at, created_by, updated_at, updated_by, is_deleted
        FROM m_wajib_pajak
        """;

    private static final RowMapper<WajibPajakResponse> MAPPER = (rs, i) ->
        WajibPajakResponse.builder()
            .id(rs.getString("id"))
            .kodeBprd(rs.getString("kode_bprd"))
            .kodeBjtm(rs.getString("kode_bjtm"))
            .namaWp(rs.getString("nama_wp"))
            .desa(rs.getString("desa"))
            .kecamatan(rs.getString("kecamatan"))
            .iupUop(rs.getString("iup_uop"))
            .komoditas(rs.getString("komoditas"))
            .ijinBerlaku(rs.getDate("ijin_berlaku") != null ? rs.getDate("ijin_berlaku").toLocalDate() : null)
            .npwp(rs.getString("npwp"))
            .statusPerizinan(rs.getObject("status_perizinan") != null && rs.getBoolean("status_perizinan"))
            .noCustomer(rs.getObject("no_customer") != null ? rs.getLong("no_customer") : null)
            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
            .createdBy(rs.getString("created_by"))
            .updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
            .updatedBy(rs.getString("updated_by"))
            .isDeleted(rs.getBoolean("is_deleted"))
            .build();

    public long count(String keyword) {
        String sql = "SELECT COUNT(*) FROM (" + SELECT_BASE + buildWhere(keyword) + ") x";
        Long c = jdbc.queryForObject(sql, buildParams(keyword), Long.class);
        return c != null ? c : 0L;
    }

    public List<WajibPajakResponse> findAll(String keyword, String sortBy, String sortType,
                                             int pageSize, int pageNumber) {
        String col   = resolveCol(sortBy);
        String order = "DESC".equalsIgnoreCase(sortType) ? "DESC" : "ASC";
        int offset   = (pageNumber - 1) * pageSize;
        String sql   = SELECT_BASE + buildWhere(keyword)
                     + " ORDER BY " + col + " " + order
                     + " LIMIT :limit OFFSET :offset";
        MapSqlParameterSource p = buildParams(keyword)
                .addValue("limit", pageSize)
                .addValue("offset", offset);
        return jdbc.query(sql, p, MAPPER);
    }

    public WajibPajakResponse findById(String id) {
        String sql = SELECT_BASE + " WHERE id = :id AND COALESCE(is_deleted, false) = false";
        List<WajibPajakResponse> r = jdbc.query(sql,
                new MapSqlParameterSource("id", id), MAPPER);
        return r.isEmpty() ? null : r.get(0);
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private String buildWhere(String keyword) {
        String where = " WHERE COALESCE(is_deleted, false) = false ";
        if (keyword != null && !keyword.isBlank())
            where += " AND CONCAT(nama_wp, kode_bprd, kode_bjtm, npwp) ILIKE :keyword ";
        return where;
    }

    private MapSqlParameterSource buildParams(String keyword) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        if (keyword != null && !keyword.isBlank())
            p.addValue("keyword", "%" + keyword + "%");
        return p;
    }

    private String resolveCol(String sortBy) {
        return switch (sortBy == null ? "" : sortBy) {
            case "namaWp"    -> "nama_wp";
            case "kodeBprd"  -> "kode_bprd";
            case "kodeBjtm"  -> "kode_bjtm";
            case "updatedAt" -> "updated_at";
            default          -> "created_at";
        };
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
