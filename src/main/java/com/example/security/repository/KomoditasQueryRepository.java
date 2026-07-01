package com.example.security.repository;

import com.example.security.dto.response.KomoditasResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Native-query repository untuk Komoditas.
 * Mengikuti pola WajibPajakQueryRepository — NamedParameterJdbcTemplate + RowMapper.
 *
 * Tidak perlu JOIN multi-tabel untuk modul ini, tapi tetap pakai pola yang sama
 * agar konsisten dan mudah ditambah kolom kalau nanti ada JOIN ke m_kelompok_komoditas.
 */
@Repository
@RequiredArgsConstructor
public class KomoditasQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    // ── Base SELECT ──────────────────────────────────────────────────────────
    private static final String SELECT_BASE = """
        SELECT id, nama, nominal, tonase,
               warna_background, warna_background_nama,
               id_kelompok_komoditas, nominal_opsen, persentase_opsen,
               created_at, created_by, updated_at, updated_by, is_deleted
        FROM m_komoditas
        """;

    // ── RowMapper ────────────────────────────────────────────────────────────
    private static final RowMapper<KomoditasResponse> MAPPER = (rs, i) ->
        KomoditasResponse.builder()
            .id(rs.getString("id"))
            .nama(rs.getString("nama"))
            .nominal(rs.getObject("nominal") != null ? rs.getInt("nominal") : null)
            .tonase(rs.getBigDecimal("tonase"))
            .warnaBackground(rs.getString("warna_background"))
            .warnaBackgroundNama(rs.getString("warna_background_nama"))
            .idKelompokKomoditas(rs.getString("id_kelompok_komoditas"))
            .nominalOpsen(rs.getObject("nominal_opsen")  != null ? rs.getDouble("nominal_opsen")  : null)
            .persentaseOpsen(rs.getObject("persentase_opsen") != null ? rs.getDouble("persentase_opsen") : null)
            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
            .createdBy(rs.getString("created_by"))
            .updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
            .updatedBy(rs.getString("updated_by"))
            .isDeleted(rs.getBoolean("is_deleted"))
            .build();

    // ── Public methods ────────────────────────────────────────────────────────

    /** Hitung total data untuk pagination meta. */
    public long count(String keyword) {
        String sql = "SELECT COUNT(*) FROM (" + SELECT_BASE + buildWhere(keyword) + ") x";
        Long c = jdbc.queryForObject(sql, buildParams(keyword), Long.class);
        return c != null ? c : 0L;
    }

    /** List dengan filter, sorting, dan pagination. */
    public List<KomoditasResponse> findAll(String keyword, String sortBy, String sortType,
                                            int pageSize, int pageNumber) {
        String col    = resolveCol(sortBy);
        String order  = "DESC".equalsIgnoreCase(sortType) ? "DESC" : "ASC";
        int    offset = (pageNumber - 1) * pageSize;
        String sql    = SELECT_BASE + buildWhere(keyword)
                      + " ORDER BY " + col + " " + order
                      + " LIMIT :limit OFFSET :offset";
        MapSqlParameterSource p = buildParams(keyword)
                .addValue("limit",  pageSize)
                .addValue("offset", offset);
        return jdbc.query(sql, p, MAPPER);
    }

    /** Ambil satu record berdasarkan ID (tanpa @SQLRestriction agar tetap bisa lihat is_deleted). */
    public KomoditasResponse findById(String id) {
        String sql = SELECT_BASE + " WHERE id = :id AND COALESCE(is_deleted, false) = false";
        List<KomoditasResponse> r = jdbc.query(sql,
                new MapSqlParameterSource("id", id), MAPPER);
        return r.isEmpty() ? null : r.get(0);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String buildWhere(String keyword) {
        String where = " WHERE COALESCE(is_deleted, false) = false ";
        if (keyword != null && !keyword.isBlank())
            where += " AND nama ILIKE :keyword ";
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
            case "nama"      -> "nama";
            case "nominal"   -> "nominal";
            case "updatedAt" -> "updated_at";
            default          -> "created_at";
        };
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
