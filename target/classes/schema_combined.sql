-- =============================================================================
-- SCHEMA GABUNGAN — Spring Security App + e-PASIR
-- =============================================================================
-- File ini menggabungkan 2 schema yang berbeda namespace tabelnya, dijalankan
-- berurutan dalam SATU transaksi:
--
--   BAGIAN A — Spring Security (sumber: schema.sql, TIDAK DIUBAH)
--     app_roles, role_permissions, users, refresh_tokens, audit_logs
--     + seed roles & user admin default + views
--
--   BAGIAN B — e-PASIR (sumber: migration_epasir_lengkap_final.sql)
--     auth_role, auth_user, m_bidang, m_pegawai, m_kartu, t_pengajuan_topup, dst.
--     Isi & urutan DROP/CREATE dipertahankan SAMA seperti file asli sesuai
--     keputusan: script ini dipakai untuk fresh install, BUKAN re-run di
--     database yang sudah berisi data produksi.
--
--   BAGIAN C — Jembatan/link antar sistem auth
--     Menambahkan kolom auth_user.id_app_user -> users.id supaya akun login
--     e-PASIR bisa ditautkan ke akun di sistem Spring Security yang baru,
--     TANPA mengubah struktur/perilaku auth_user maupun users yang sudah ada.
--
-- KEAMANAN — tidak ada satupun statement di bawah yang menyentuh tabel:
--   app_roles, role_permissions, users, refresh_tokens, audit_logs
-- Seluruh "DROP TABLE IF EXISTS" pada Bagian B hanya menyasar tabel-tabel
-- e-PASIR (nama berbeda total dari tabel Spring Security di atas), sehingga
-- security/login yang sudah Anda buat sebelumnya tidak akan terhapus/berubah.
--
-- CARA JALANKAN (satu kali, di database baru/kosong):
--   psql -U postgres -d security_db -f schema_combined.sql
-- =============================================================================

-- Jalankan sebagai satu transaksi: kalau ada error di tengah jalan, semua
-- di-rollback otomatis sehingga tidak ada schema "setengah jadi".
BEGIN;

-- =============================================================================
-- BAGIAN A — SPRING SECURITY (persis dari schema.sql, tidak diubah)
-- =============================================================================

-- ============================================================
-- SCHEMA LENGKAP v2 — Spring Security App
-- Menggabungkan v1 (auth) + v2 (role management & permissions)
-- Jalankan sebagai: psql -U postgres -d security_db -f schema.sql
-- ============================================================

-- ── 1. TABEL APP_ROLES (harus sebelum users) ──────────────────────────────
CREATE TABLE IF NOT EXISTS app_roles (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(50)  NOT NULL UNIQUE,
    description    VARCHAR(200),
    color          VARCHAR(10)  NOT NULL DEFAULT '#1A237E',
    is_system_role BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_role_name ON app_roles(name);

-- ── 2. TABEL ROLE_PERMISSIONS ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id    UUID        NOT NULL REFERENCES app_roles(id) ON DELETE CASCADE,
    permission VARCHAR(60) NOT NULL,
    PRIMARY KEY (role_id, permission)
);

-- ── 3. TABEL USERS ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username              VARCHAR(50)  NOT NULL UNIQUE,
    email                 VARCHAR(100) NOT NULL UNIQUE,
    password              VARCHAR(255) NOT NULL,
    full_name             VARCHAR(100),
    phone_number          VARCHAR(20),
    role_id               UUID         REFERENCES app_roles(id),
    is_enabled            BOOLEAN      NOT NULL DEFAULT TRUE,
    is_account_non_locked BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_login_attempts INTEGER      NOT NULL DEFAULT 0,
    lock_time             TIMESTAMP,
    last_login            TIMESTAMP,
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email    ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_role_id  ON users(role_id);

-- ── 4. TABEL REFRESH_TOKENS ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    token       VARCHAR(512) NOT NULL UNIQUE,
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at  TIMESTAMP    NOT NULL,
    is_revoked  BOOLEAN      NOT NULL DEFAULT FALSE,
    device_info VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_value ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user  ON refresh_tokens(user_id);

-- ── 5. TABEL AUDIT_LOGS ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_logs (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(100),
    action        VARCHAR(100) NOT NULL,
    ip_address    VARCHAR(50),
    user_agent    VARCHAR(255),
    resource_path VARCHAR(255),
    status        VARCHAR(20),
    details       VARCHAR(500),
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_username   ON audit_logs(username);
CREATE INDEX IF NOT EXISTS idx_audit_action     ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs(created_at DESC);

-- ── 6. SEED ROLES DEFAULT ─────────────────────────────────────────────────

INSERT INTO app_roles (id, name, description, color, is_system_role)
VALUES
  ('11111111-1111-1111-1111-111111111111',
   'ROLE_ADMIN', 'Administrator dengan akses penuh ke semua fitur sistem', '#B71C1C', TRUE),
  ('22222222-2222-2222-2222-222222222222',
   'ROLE_MODERATOR', 'Moderator dengan akses baca dan kunci pengguna', '#1565C0', TRUE),
  ('33333333-3333-3333-3333-333333333333',
   'ROLE_USER', 'Pengguna standar dengan akses terbatas', '#2E7D32', TRUE)
ON CONFLICT (name) DO NOTHING;

-- Permissions untuk ROLE_ADMIN (semua)
INSERT INTO role_permissions (role_id, permission)
SELECT '11111111-1111-1111-1111-111111111111', p FROM unnest(ARRAY[
  'USER_VIEW','USER_CREATE','USER_EDIT','USER_DELETE','USER_LOCK','USER_ASSIGN_ROLE',
  'ROLE_VIEW','ROLE_CREATE','ROLE_EDIT','ROLE_DELETE','ROLE_ASSIGN_PERMISSION',
  'AUDIT_VIEW','AUDIT_EXPORT','DASHBOARD_VIEW','DASHBOARD_STATS'
]) AS p ON CONFLICT DO NOTHING;

-- Permissions untuk ROLE_MODERATOR
INSERT INTO role_permissions (role_id, permission)
SELECT '22222222-2222-2222-2222-222222222222', p FROM unnest(ARRAY[
  'USER_VIEW','USER_LOCK','ROLE_VIEW','AUDIT_VIEW','DASHBOARD_VIEW','DASHBOARD_STATS'
]) AS p ON CONFLICT DO NOTHING;

-- Permissions untuk ROLE_USER
INSERT INTO role_permissions (role_id, permission)
SELECT '33333333-3333-3333-3333-333333333333', p FROM unnest(ARRAY[
  'DASHBOARD_VIEW'
]) AS p ON CONFLICT DO NOTHING;

-- ── 7. SEED USER ADMIN DEFAULT ────────────────────────────────────────────
-- Password: Admin@1234 (BCrypt strength 12)
INSERT INTO users (username, email, password, full_name, role_id, created_at)
VALUES (
  'admin', 'admin@example.com',
  '$2a$12$LcVKhx2R7eDe5V1X0h9B/.gX.JaEqVWLh1M7UFLbTkw/P4dOBdxXi',
  'System Administrator',
  '11111111-1111-1111-1111-111111111111',
  NOW()
) ON CONFLICT (username) DO NOTHING;

-- ── 8. VIEWS ─────────────────────────────────────────────────────────────

CREATE OR REPLACE VIEW v_role_user_counts AS
SELECT r.id, r.name, r.description, r.color, r.is_system_role,
       COUNT(u.id) AS user_count
FROM app_roles r
LEFT JOIN users u ON u.role_id = r.id
GROUP BY r.id, r.name, r.description, r.color, r.is_system_role;

CREATE OR REPLACE VIEW v_active_sessions AS
SELECT u.username, u.email, COUNT(rt.id) AS active_token_count,
       MAX(rt.created_at) AS last_token_created
FROM users u
LEFT JOIN refresh_tokens rt ON rt.user_id = u.id
    AND rt.is_revoked = FALSE AND rt.expires_at > NOW()
GROUP BY u.id, u.username, u.email;

CREATE OR REPLACE VIEW v_security_events AS
SELECT action, status, COUNT(*) AS event_count, DATE(created_at) AS event_date
FROM audit_logs
WHERE created_at >= NOW() - INTERVAL '30 days'
GROUP BY action, status, DATE(created_at)
ORDER BY event_date DESC, event_count DESC;


-- =============================================================================
-- BAGIAN B — e-PASIR (dari migration_epasir_lengkap_final.sql)
-- =============================================================================
-- Catatan: blok di bawah ini disalin LENGKAP dari file migrasi asli, termasuk
-- seluruh "DROP TABLE IF EXISTS" / "DROP FUNCTION IF EXISTS" pada tabel-tabel
-- e-PASIR (sesuai keputusan: dipakai untuk fresh install, tidak idempotent
-- terhadap data lama e-PASIR).
--
-- PAGAR PENGAMAN: guard ini menghentikan eksekusi LEBIH DAHULU jika nama
-- tabel Spring Security tidak terdeteksi, mencegah skenario file ini
-- ter-paste/dijalankan sendirian di database yang salah dan secara tidak
-- sengaja men-drop sesuatu yang tidak diinginkan. Ini TIDAK mengubah isi
-- migrasi e-PASIR itu sendiri.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'users'
  ) THEN
    RAISE EXCEPTION
      'Guard: tabel "users" (Spring Security) tidak ditemukan. Pastikan BAGIAN A sudah dijalankan lebih dulu di database yang benar sebelum melanjutkan BAGIAN B.';
  END IF;
END $$;

-- =============================================================================
-- MIGRATION FINAL LENGKAP: Sistem Baru e-PASIR
-- Fitur: Login (auth) + Transaksi Pengajuan TopUp
-- Stack Baru: Java Spring Boot + PostgreSQL
-- Sumber: e-pasir-reborn (sistem lama Golang/Nuxt)
-- =============================================================================
--
-- PEMETAAN SEMUA TABEL SISTEM LAMA vs KEBUTUHAN SISTEM BARU:
--
-- [DIPAKAI - masuk migration ini]
--   auth_role              → login: master role
--   auth_user              → login: data user
--   m_bidang               → login: join via m_unit_organisasi_kerja
--   m_unit_organisasi_kerja→ login: join di auth_user query (id_unor)
--   m_golongan             → login: FK m_pegawai
--   m_jabatan              → login: FK m_pegawai
--   m_fungsionalitas       → login: join di auth_user query (id_fungsionalitas)
--   m_pegawai              → login: join di auth_user query (id_pegawai)
--   menu                   → login: menu navigasi per role
--   menu_user              → login: mapping menu ke role
--   m_kelompok_komoditas   → master parent komoditas
--   m_komoditas            → topup: join detail kartu (nama komoditas)
--   m_wajib_pajak          → topup: join header (nama_wp, iup_uop, npwp)
--   m_kartu                → topup: data kartu RFID + saldo_bprd
--   m_lokasi               → login: kolom id_lokasi di auth_user
--   t_pengajuan_topup      → topup: tabel header transaksi
--   t_pengajuan_topup_detail → topup: tabel detail per kartu
--   t_skab                 → topup: dipakai di fn_list_history_kartu &
--                                   dashboard (digunakan sistem baru)
--   fn_get_no_topup()      → topup: auto-generate nomor pengajuan
--   fn_list_history_kartu()→ topup: history debet/kredit per kartu
--                                   (dipanggil di nomor.repository.go)
--
-- [TIDAK DIPAKAI - tidak masuk migration ini]
--   kartu_excel            → tabel temp import kartu via Excel, tidak ada
--                            domain/repository yang menggunakannya di sistem baru
--   m_nomor                → tabel lama sebelum diganti m_kartu, tidak ada
--                            query ke m_nomor di sistem baru (semua pakai m_kartu)
--   m_armada               → tidak ada domain/repository yang mengaksesnya
--   m_desa, m_kecamatan    → tidak dipakai sistem baru (data wilayah statis)
--   m_jenis_kelamin        → tidak ada domain yang mengaksesnya
--   m_jenis_biaya          → domain BPD perjalanan dinas, bukan topup
--   m_jenis_kendaraan      → hanya dipakai domain JenisKendaraan (fitur lain)
--   c_jenis_kendaraan      → hanya dipakai gate.repository (fitur gate/tapping)
--   config_jenis_kendaraan → tidak ada domain yang mengaksesnya di sistem baru
--   history_t_skab         → tabel log trigger otomatis dari t_skab, tidak
--                            ada repository yang insert langsung ke sini
--   log_activity           → fitur log aktivitas user (bukan fitur topup)
--   log_kegiatan           → perjalanan dinas (bukan fitur topup)
--   app_config             → konfigurasi aplikasi (tidak kritis untuk topup)
--   auth_users             → tabel user sistem lain (berbeda dengan auth_user)
--   rekon_va               → rekonsiliasi VA (fitur lain)
--   t_pelimpahan_rkud      → pelimpahan ke RKUD (fitur lain)
--   t_pelimpahan_rkud_detail
--   t_penyesuaian          → penyesuaian saldo (fitur lain)
--   t_rekonsiliasi_va      → rekonsiliasi VA (fitur lain)
--   t_sinkron_saldo        → sinkronisasi saldo (fitur lain)
--   t_sinkron_saldo_detail
--   t_skab_emergency       → transaksi emergency (fitur lain)
--   t_sptpd, t_sptpd_detail→ SPTPD pajak (fitur lain)
--   t_va_tmp               → tabel temporary VA
--   target_skab            → target/quota tapping (fitur lain)
--   update_kartu_ma5       → tabel temp update kartu (tidak dipakai)
--   porporasi, porporasi_detail → fitur porporasi (fitur lain)
--   m_kendaraan            → kendaraan operasional (bukan topup)
--   m_agama                → data agama pegawai (tidak dipakai sistem baru)
--
-- Urutan eksekusi (dependency order):
--   1.  Extension + uuid_generate_v1
--   2.  m_bidang
--   3.  m_unit_organisasi_kerja  (→ m_bidang)
--   4.  m_golongan
--   5.  m_jabatan
--   6.  m_fungsionalitas
--   7.  m_pegawai               (→ m_unit_organisasi_kerja, m_golongan, m_jabatan)
--   8.  auth_role
--   9.  auth_user               (→ auth_role, m_pegawai, m_fungsionalitas)
--   10. menu
--   11. menu_user               (→ menu, auth_role)
--   12. m_lokasi
--   13. m_kelompok_komoditas
--   14. m_komoditas             (→ m_kelompok_komoditas)
--   15. m_wajib_pajak
--   16. m_kartu                 (→ m_wajib_pajak, m_komoditas)
--   17. t_skab                  (→ m_wajib_pajak, m_kartu)
--   18. fn_get_no_topup()
--   19. t_pengajuan_topup       (→ m_wajib_pajak)
--   20. t_pengajuan_topup_detail(→ t_pengajuan_topup)
--   21. fn_list_history_kartu() (→ t_skab, t_pengajuan_topup_detail, m_kartu, m_wajib_pajak)
--   22. Primary Key & Unique Constraint
--   23. Index
--   24. Foreign Key
-- =============================================================================

-- =============================================================================
-- 1. EXTENSION & FUNCTION uuid_generate_v1
-- =============================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Catatan perbaikan: baris "DROP FUNCTION IF EXISTS uuid_generate_v1()" pada
-- file migrasi asli dihapus karena fungsi ini dimiliki oleh extension
-- "uuid-ossp" dan tidak bisa di-drop terpisah (akan error: "cannot drop
-- function uuid_generate_v1() because extension uuid-ossp requires it").
-- CREATE OR REPLACE di bawah ini sudah cukup, tidak perlu drop dulu.
CREATE OR REPLACE FUNCTION "public"."uuid_generate_v1"()
  RETURNS uuid AS '$libdir/uuid-ossp', 'uuid_generate_v1'
  LANGUAGE c VOLATILE STRICT COST 1;


-- =============================================================================
-- 2. m_bidang
--    Master bidang/divisi; dipakai oleh m_unit_organisasi_kerja
-- =============================================================================
DROP TABLE IF EXISTS "public"."m_bidang";
CREATE TABLE "public"."m_bidang" (
  "id"         varchar(36) NOT NULL,
  "kode"       varchar(50),
  "nama"       varchar,
  "created_at" timestamp(6),
  "created_by" varchar,
  "updated_at" timestamp(6),
  "updated_by" varchar,
  "is_deleted" bool DEFAULT false
);
COMMENT ON TABLE "public"."m_bidang" IS 'Master bidang / divisi organisasi';


-- =============================================================================
-- 3. m_unit_organisasi_kerja
--    Master unit kerja; dipakai oleh m_pegawai dan auth_user (id_unor)
-- =============================================================================
DROP TABLE IF EXISTS "public"."m_unit_organisasi_kerja";
CREATE TABLE "public"."m_unit_organisasi_kerja" (
  "id"         varchar(36)  NOT NULL,
  "kode"       varchar(50)  NOT NULL,
  "nama"       varchar(255),
  "id_bidang"  varchar(36),
  "created_at" timestamp(6),
  "created_by" varchar,
  "updated_at" timestamp(6),
  "updated_by" varchar,
  "is_deleted" bool DEFAULT false
);
COMMENT ON TABLE "public"."m_unit_organisasi_kerja" IS 'Master unit organisasi kerja (unor)';


-- =============================================================================
-- 4. m_golongan
--    Master golongan pegawai; dipakai FK m_pegawai
-- =============================================================================
DROP TABLE IF EXISTS "public"."m_golongan";
CREATE TABLE "public"."m_golongan" (
  "id"         varchar(36) NOT NULL,
  "kode"       varchar(10),
  "nama"       varchar(255),
  "created_at" timestamp(6),
  "updated_at" timestamp(6),
  "created_by" varchar(36),
  "updated_by" varchar(36),
  "is_deleted" bool DEFAULT false
);
COMMENT ON TABLE "public"."m_golongan" IS 'Master golongan kepangkatan pegawai';


-- =============================================================================
-- 5. m_jabatan
--    Master jabatan pegawai; dipakai FK m_pegawai
-- =============================================================================
DROP TABLE IF EXISTS "public"."m_jabatan";
CREATE TABLE "public"."m_jabatan" (
  "id"         varchar(36) NOT NULL,
  "nama"       varchar(255),
  "created_at" timestamp(6),
  "updated_at" timestamp(6),
  "created_by" varchar(36),
  "updated_by" varchar(36),
  "is_deleted" bool DEFAULT false
);
COMMENT ON TABLE "public"."m_jabatan" IS 'Master jabatan pegawai';


-- =============================================================================
-- 6. m_fungsionalitas
--    Level fungsionalitas user; dipakai JOIN di auth_user query
-- =============================================================================
DROP TABLE IF EXISTS "public"."m_fungsionalitas";
CREATE TABLE "public"."m_fungsionalitas" (
  "id"         varchar(36) NOT NULL DEFAULT uuid_generate_v1(),
  "nama"       varchar(255),
  "level"      int4,
  "created_at" timestamp(6),
  "updated_at" timestamp(6),
  "created_by" varchar(36),
  "updated_by" varchar(36),
  "is_deleted" bool DEFAULT false
);
COMMENT ON TABLE "public"."m_fungsionalitas" IS 'Master level fungsionalitas / jabatan fungsional user';


-- =============================================================================
-- 7. m_pegawai
--    Data pegawai internal; dipakai JOIN di auth_user query (id_pegawai)
-- =============================================================================
DROP TABLE IF EXISTS "public"."m_pegawai";
CREATE TABLE "public"."m_pegawai" (
  "id"                varchar(36) NOT NULL,
  "nip"               varchar(20),
  "nama"              varchar(255),
  "jenis_kelamin"     varchar(2),
  "agama"             varchar(36),
  "alamat"            text,
  "no_hp"             varchar(36),
  "email"             varchar,
  "id_unor"           varchar(36),
  "id_jabatan"        varchar(36),
  "id_golongan"       varchar(36),
  "created_at"        timestamp(6),
  "updated_at"        timestamp(6),
  "created_by"        varchar(36),
  "updated_by"        varchar(36),
  "is_deleted"        bool DEFAULT false,
  "foto"              varchar,
  "id_fungsionalitas" varchar(36)
);
COMMENT ON TABLE "public"."m_pegawai" IS 'Master data pegawai internal';


-- =============================================================================
-- 8. auth_role
--    Master role/hak akses; dipakai FK di auth_user dan menu_user
-- =============================================================================
DROP TABLE IF EXISTS "public"."auth_role";
CREATE TABLE "public"."auth_role" (
  "id"                 varchar(255) NOT NULL,
  "nama"               varchar(255),
  "keterangan"         varchar(255) NOT NULL,
  "is_view_data_all"   bool,
  "is_choose_pegawai"  bool,
  "is_choose_terbatas" bool
);
COMMENT ON TABLE  "public"."auth_role"                         IS 'Master role / hak akses user';
COMMENT ON COLUMN "public"."auth_role"."is_view_data_all"     IS 'true = dapat melihat data semua WP';
COMMENT ON COLUMN "public"."auth_role"."is_choose_pegawai"    IS 'true = dapat memilih pegawai';
COMMENT ON COLUMN "public"."auth_role"."is_choose_terbatas"   IS 'true = akses data dibatasi per unit';


-- =============================================================================
-- 9. auth_user
--    Data user untuk login; join ke auth_role, m_pegawai, m_fungsionalitas,
--    m_unit_organisasi_kerja, m_bidang, m_wajib_pajak
-- =============================================================================
DROP TABLE IF EXISTS "public"."auth_user";
CREATE TABLE "public"."auth_user" (
  "id"                varchar(36)  NOT NULL,
  "nama"              varchar(255),
  "username"          varchar(255) NOT NULL,
  "email"             varchar(254) NOT NULL,
  "password"          varchar(60)  NOT NULL,
  "status"            varchar(1),
  "id_role"           varchar(36)  NOT NULL,
  "id_unor"           varchar(36),
  "id_fungsionalitas" varchar(36),
  "id_pegawai"        varchar(36),
  "foto"              varchar,
  "active"            bool         DEFAULT true,
  "created_at"        timestamptz(6),
  "updated_at"        timestamptz(6),
  "deleted_at"        timestamptz(6),
  "is_deleted"        bool         DEFAULT false,
  "id_wp"             varchar(36),
  "device_id"         text,
  "id_lokasi"         varchar(36),
  "is_emergency"      bool         DEFAULT false
);
COMMENT ON TABLE  "public"."auth_user"                IS 'Data user untuk autentikasi login';
COMMENT ON COLUMN "public"."auth_user"."password"     IS 'BCrypt hash password (length 60)';
COMMENT ON COLUMN "public"."auth_user"."status"       IS '1 = Aktif, 0 = Non-aktif';
COMMENT ON COLUMN "public"."auth_user"."id_wp"        IS 'Diisi jika user adalah wajib pajak';
COMMENT ON COLUMN "public"."auth_user"."is_emergency" IS 'true = user memiliki akses mode emergency';


-- =============================================================================
-- 10. menu
--     Master menu navigasi aplikasi
-- =============================================================================
DROP TABLE IF EXISTS "public"."menu";
CREATE TABLE "public"."menu" (
  "id"            varchar     NOT NULL,
  "nama_menu"     varchar,
  "link_menu"     varchar,
  "keterangan"    varchar,
  "class_icon"    varchar,
  "status"        varchar(1),
  "created_at"    timestamp(6) DEFAULT now(),
  "updated_at"    timestamp(6),
  "is_permission" bool,
  "is_deleted"    bool         DEFAULT false,
  "app"           int4
);
COMMENT ON TABLE  "public"."menu"           IS 'Master menu navigasi aplikasi';
COMMENT ON COLUMN "public"."menu"."status"  IS '1 = Aktif, 0 = Non-aktif';
COMMENT ON COLUMN "public"."menu"."app"     IS 'Kode aplikasi (untuk multi-app)';


-- =============================================================================
-- 11. menu_user
--     Mapping menu ke role; dipakai role.repository untuk akses kontrol menu
-- =============================================================================
DROP TABLE IF EXISTS "public"."menu_user";
CREATE TABLE "public"."menu_user" (
  "id"         varchar     NOT NULL,
  "id_menu"    varchar,
  "posisi"     varchar(1),
  "level"      int4,
  "urutan"     int4,
  "status"     varchar(1),
  "created_at" timestamp(6) DEFAULT now(),
  "updated_at" timestamp(6),
  "parent"     varchar,
  "id_role"    varchar,
  "is_deleted" bool         DEFAULT false
);
COMMENT ON TABLE  "public"."menu_user"          IS 'Mapping menu navigasi ke role user';
COMMENT ON COLUMN "public"."menu_user"."posisi" IS 'Posisi tampilan menu (L=Left, dll)';
COMMENT ON COLUMN "public"."menu_user"."level"  IS 'Level kedalaman menu (1=root, 2=child, dst)';
COMMENT ON COLUMN "public"."menu_user"."parent" IS 'ID menu parent untuk menu bertingkat';


-- =============================================================================
-- 12. m_lokasi
--     Master lokasi gate/pos; kolom id_lokasi ada di auth_user dan t_skab
-- =============================================================================
DROP TABLE IF EXISTS "public"."m_lokasi";
CREATE TABLE "public"."m_lokasi" (
  "id"         varchar(36) NOT NULL DEFAULT uuid_generate_v1(),
  "nama"       varchar(200),
  "keterangan" text,
  "created_at" timestamp(6) DEFAULT now(),
  "create_by"  varchar(36),
  "is_deleted" bool DEFAULT false,
  "kode"       varchar,
  "lat"        float8,
  "lng"        float8
);
COMMENT ON TABLE "public"."m_lokasi" IS 'Master lokasi gate/pos tapping';


-- =============================================================================
-- 13. m_kelompok_komoditas
--     Parent kategori komoditas
-- =============================================================================
DROP TABLE IF EXISTS "public"."m_kelompok_komoditas";
CREATE TABLE "public"."m_kelompok_komoditas" (
  "id"         varchar(36) NOT NULL DEFAULT uuid_generate_v1(),
  "nama"       varchar(100),
  "created_at" timestamp(6),
  "created_by" varchar(36),
  "updated_at" timestamp(6),
  "updated_by" varchar(36),
  "is_deleted" bool DEFAULT false
);
COMMENT ON TABLE "public"."m_kelompok_komoditas" IS 'Kelompok / kategori komoditas';


-- =============================================================================
-- 14. m_komoditas
--     Jenis komoditas (pasir, batu, dll); dipakai JOIN di query kartu & detail topup
-- =============================================================================
DROP TABLE IF EXISTS "public"."m_komoditas";
CREATE TABLE "public"."m_komoditas" (
  "id"                    varchar(36) NOT NULL DEFAULT uuid_generate_v1(),
  "nama"                  varchar(100),
  "nominal"               int4,
  "created_at"            timestamp(6),
  "created_by"            varchar(36),
  "updated_at"            timestamp(6),
  "updated_by"            varchar(36),
  "is_deleted"            bool DEFAULT false,
  "tonase"                numeric,
  "warna_background"      varchar,
  "warna_background_nama" varchar,
  "id_kelompok_komoditas" varchar(36),
  "nominal_opsen"         float8,
  "persentase_opsen"      float8
);
COMMENT ON TABLE  "public"."m_komoditas"                   IS 'Master jenis komoditas / material tambang';
COMMENT ON COLUMN "public"."m_komoditas"."nominal"         IS 'Tarif pajak per satuan (Rp)';
COMMENT ON COLUMN "public"."m_komoditas"."tonase"          IS 'Kapasitas tonase per trip';
COMMENT ON COLUMN "public"."m_komoditas"."nominal_opsen"   IS 'Nilai opsen pajak (Rp)';
COMMENT ON COLUMN "public"."m_komoditas"."persentase_opsen" IS 'Persentase opsen pajak (%)';


-- =============================================================================
-- 15. m_wajib_pajak
--     Master wajib pajak / penambang; dipakai JOIN di topup header, kartu, t_skab
-- =============================================================================
DROP TABLE IF EXISTS "public"."m_wajib_pajak";
CREATE TABLE "public"."m_wajib_pajak" (
  "id"               varchar(36)  NOT NULL DEFAULT uuid_generate_v1(),
  "kode_bprd"        varchar(100),
  "kode_bjtm"        varchar(100),
  "nama_wp"          varchar(200),
  "desa"             varchar(200),
  "kecamatan"        varchar(200),
  "iup_uop"          varchar(200),
  "komoditas"        varchar(200),
  "ijin_berlaku"     date,
  "npwp"             varchar(100),
  "created_at"       timestamp(6),
  "created_by"       varchar(36),
  "updated_at"       timestamp(6),
  "updated_by"       varchar(36),
  "is_deleted"       bool         DEFAULT false,
  "id_desa"          varchar(36),
  "status_perizinan" bool         DEFAULT true,
  "id_op"            varchar,
  "nama"             varchar,
  "no_customer"      int8
);
COMMENT ON TABLE  "public"."m_wajib_pajak"                    IS 'Master data wajib pajak / penambang';
COMMENT ON COLUMN "public"."m_wajib_pajak"."kode_bprd"        IS 'Kode internal BPRD';
COMMENT ON COLUMN "public"."m_wajib_pajak"."kode_bjtm"        IS 'Kode nasabah Bank Jatim';
COMMENT ON COLUMN "public"."m_wajib_pajak"."iup_uop"          IS 'Nomor Izin Usaha Pertambangan';
COMMENT ON COLUMN "public"."m_wajib_pajak"."status_perizinan" IS 'true = izin aktif, false = izin tidak aktif';
COMMENT ON COLUMN "public"."m_wajib_pajak"."no_customer"      IS 'Nomor customer Bank Jatim';


-- =============================================================================
-- 16. m_kartu
--     Kartu RFID milik wajib pajak; saldo_bprd diupdate saat topup diapprove
-- =============================================================================
DROP TABLE IF EXISTS "public"."m_kartu";
CREATE TABLE "public"."m_kartu" (
  "id"              varchar(36)  NOT NULL DEFAULT uuid_generate_v1(),
  "no_rfid"         varchar(100),
  "kode_kartu"      varchar(100),
  "no_va"           varchar(100),
  "nama_wp"         varchar(200),
  "created_at"      timestamp(6),
  "created_by"      varchar(36),
  "updated_at"      timestamp(6),
  "updated_by"      varchar(36),
  "is_deleted"      bool         DEFAULT false,
  "id_wp"           varchar(36),
  "id_komoditas"    varchar(36),
  "saldo"           float8,
  "armada"          varchar(255),
  "nopol"           varchar(10),
  "tujuan"          varchar(100),
  "keterangan"      text,
  "saldo_bprd"      float8       DEFAULT 0,
  "aktif"           bool         DEFAULT true,
  "encrypt_va"      text,
  "tgl_pendaftaran" timestamp(6),
  "porporasi"       bool         DEFAULT false,
  "last_topup"      timestamp(6)
);
COMMENT ON TABLE  "public"."m_kartu"              IS 'Master kartu RFID milik wajib pajak';
COMMENT ON COLUMN "public"."m_kartu"."no_rfid"    IS 'Nomor RFID unik pada kartu fisik';
COMMENT ON COLUMN "public"."m_kartu"."no_va"      IS 'Nomor Virtual Account Bank Jatim';
COMMENT ON COLUMN "public"."m_kartu"."saldo"      IS 'Saldo aktif (berkurang tiap tapping gate)';
COMMENT ON COLUMN "public"."m_kartu"."saldo_bprd" IS 'Saldo BPRD, diupdate saat topup diapprove';
COMMENT ON COLUMN "public"."m_kartu"."aktif"      IS 'true = kartu aktif dapat digunakan';
COMMENT ON COLUMN "public"."m_kartu"."porporasi"  IS 'true = kartu terdaftar skema porporasi';
COMMENT ON COLUMN "public"."m_kartu"."last_topup" IS 'Timestamp topup terakhir berhasil';


-- =============================================================================
-- 17. t_skab
--     Transaksi tapping/debet kartu di gate; dipakai dashboard, laporan,
--     dan fn_list_history_kartu (history debet kartu)
-- =============================================================================
DROP TABLE IF EXISTS "public"."t_skab";
CREATE TABLE "public"."t_skab" (
  "id"               varchar(36)  NOT NULL DEFAULT uuid_generate_v1(),
  "tanggal"          date,
  "no_rfid"          varchar(100),
  "no_va"            varchar(100),
  "nominal"          int8,
  "nama_wp"          varchar(200),
  "time_taping"      timestamp(6),
  "gate_taping"      varchar(100),
  "created_at"       timestamp(6) DEFAULT now(),
  "kode_bprd"        varchar(100),
  "kode_bjtm"        varchar(100),
  "trx_no"           varchar(30),
  "id_wp"            varchar(36),
  "id_operator"      varchar(36),
  "reffno"           varchar(100),
  "keterangan"       varchar,
  "is_cron"          bool,
  "latitude"         float8,
  "longitude"        float8,
  "armada"           varchar(255),
  "nopol"            varchar(10),
  "id_lokasi"        varchar(36),
  "tonase"           numeric,
  "sudah_cetak"      bool         DEFAULT false,
  "nominal_opsen"    int4,
  "persentase_opsen" float8,
  "is_setoran"       bool,
  "on_rkud"          bool,
  "updated_at"       timestamp(6),
  "updated_by"       varchar(36),
  "path_file"        text,
  "reason"           text,
  "is_deleted"       bool         DEFAULT false
);
COMMENT ON TABLE  "public"."t_skab"                IS 'Transaksi tapping/debet kartu RFID di gate';
COMMENT ON COLUMN "public"."t_skab"."nominal"      IS 'Nominal pajak per trip (Rp)';
COMMENT ON COLUMN "public"."t_skab"."trx_no"       IS 'Nomor transaksi unik';
COMMENT ON COLUMN "public"."t_skab"."gate_taping"  IS 'Identifikasi gate yang melakukan tapping';
COMMENT ON COLUMN "public"."t_skab"."time_taping"  IS 'Waktu tapping kartu di gate';
COMMENT ON COLUMN "public"."t_skab"."is_cron"      IS 'true = data diinsert via cron job otomatis';
COMMENT ON COLUMN "public"."t_skab"."sudah_cetak"  IS 'true = struk/kuitansi sudah dicetak';
COMMENT ON COLUMN "public"."t_skab"."nominal_opsen" IS 'Nilai opsen pajak per trip (Rp)';
COMMENT ON COLUMN "public"."t_skab"."on_rkud"      IS 'true = sudah dilimpahkan ke RKUD';


-- =============================================================================
-- 18. FUNCTION: fn_get_no_topup
--     Auto-generate nomor urut pengajuan topup: 0001/DD-MM/BPRD/YYYY
--     HARUS dibuat sebelum t_pengajuan_topup karena dipakai sebagai DEFAULT
-- =============================================================================
DROP FUNCTION IF EXISTS "public"."fn_get_no_topup"();
CREATE OR REPLACE FUNCTION "public"."fn_get_no_topup"()
  RETURNS varchar AS $BODY$
DECLARE
  v_nomor varchar;
BEGIN
  SELECT max(split_part(nomor, '/', 1)) INTO v_nomor
    FROM t_pengajuan_topup
   WHERE nomor LIKE '%' || to_char(current_date::date, 'yyyy');

  RETURN coalesce(trim(to_char(coalesce(v_nomor::int, 0)::int + 1, '0000')), '0001')
         || '/' || to_char(current_date::date, 'DD-MM')
         || '/' || 'BPRD/'
         || to_char(current_date::date, 'YYYY');
END
$BODY$
  LANGUAGE plpgsql VOLATILE COST 100;
COMMENT ON FUNCTION "public"."fn_get_no_topup"()
  IS 'Generate nomor urut pengajuan topup, format: 0001/DD-MM/BPRD/YYYY';


-- =============================================================================
-- 19. t_pengajuan_topup
--     Header transaksi pengajuan topup saldo kartu RFID
-- =============================================================================
DROP TABLE IF EXISTS "public"."t_pengajuan_topup";
CREATE TABLE "public"."t_pengajuan_topup" (
  "id"          varchar(36)  NOT NULL DEFAULT uuid_generate_v1(),
  "tanggal"     date,
  "id_wp"       varchar(36),
  "nominal"     float8,
  "file_bukti"  text,
  "created_at"  timestamp(6) DEFAULT now(),
  "updated_at"  timestamp(6),
  "created_by"  varchar,
  "updated_by"  varchar,
  "is_deleted"  bool         DEFAULT false,
  "status"      varchar(2),
  "verified_at" timestamp(6),
  "nomor"       varchar(50)  DEFAULT fn_get_no_topup()
);
COMMENT ON TABLE  "public"."t_pengajuan_topup"              IS 'Header transaksi pengajuan topup saldo kartu RFID';
COMMENT ON COLUMN "public"."t_pengajuan_topup"."nominal"    IS 'Total nominal topup seluruh kartu dalam pengajuan';
COMMENT ON COLUMN "public"."t_pengajuan_topup"."file_bukti" IS 'Path/URL file bukti transfer bank';
COMMENT ON COLUMN "public"."t_pengajuan_topup"."status"     IS '1 = Proses, 2 = Diapprove, 3 = Ditolak';
COMMENT ON COLUMN "public"."t_pengajuan_topup"."verified_at" IS 'Timestamp saat diapprove / ditolak';
COMMENT ON COLUMN "public"."t_pengajuan_topup"."nomor"      IS 'Nomor pengajuan, auto-generate: 0001/DD-MM/BPRD/YYYY';


-- =============================================================================
-- 20. t_pengajuan_topup_detail
--     Detail per kartu RFID dalam satu pengajuan topup
-- =============================================================================
DROP TABLE IF EXISTS "public"."t_pengajuan_topup_detail";
CREATE TABLE "public"."t_pengajuan_topup_detail" (
  "id"                  varchar(36)  NOT NULL,
  "id_pengajuan_topup"  varchar(36),
  "no_rfid"             varchar(100),
  "no_va"               varchar(100),
  "nominal"             float8,
  "status_approve_bjtm" varchar(10),
  "created_at"          timestamp(6) DEFAULT now(),
  "updated_at"          timestamp(6)
);
COMMENT ON TABLE  "public"."t_pengajuan_topup_detail"                        IS 'Detail pengajuan topup per kartu RFID';
COMMENT ON COLUMN "public"."t_pengajuan_topup_detail"."id_pengajuan_topup"   IS 'FK ke t_pengajuan_topup.id';
COMMENT ON COLUMN "public"."t_pengajuan_topup_detail"."no_rfid"              IS 'Nomor RFID kartu (ref ke m_kartu.no_rfid)';
COMMENT ON COLUMN "public"."t_pengajuan_topup_detail"."no_va"                IS 'Nomor Virtual Account Bank Jatim';
COMMENT ON COLUMN "public"."t_pengajuan_topup_detail"."nominal"              IS 'Nominal topup untuk kartu ini';
COMMENT ON COLUMN "public"."t_pengajuan_topup_detail"."status_approve_bjtm"  IS '1 = Proses, 2 = Diapprove (saldo ditambahkan ke m_kartu.saldo_bprd), 3 = Ditolak';


-- =============================================================================
-- 21. FUNCTION: fn_list_history_kartu
--     History debet (t_skab) + kredit (t_pengajuan_topup_detail) per kartu RFID
--     Dipanggil di nomor.repository.go → SelectHistory
-- =============================================================================
DROP FUNCTION IF EXISTS "public"."fn_list_history_kartu"("p_no_rfid" varchar);
CREATE OR REPLACE FUNCTION "public"."fn_list_history_kartu"("p_no_rfid" varchar)
  RETURNS SETOF "pg_catalog"."record" AS $BODY$
DECLARE
  rcd record;
BEGIN
  FOR rcd IN
    SELECT * FROM (
      -- DEBET: transaksi tapping di gate (pengurangan saldo)
      SELECT x.* FROM (
        SELECT
          a.time_taping::timestamp without time zone  AS time_tapping,
          a.no_rfid,
          a.no_va,
          mk.kode_kartu,
          a.nominal,
          a.trx_no,
          a.gate_taping::varchar                      AS gate_taping,
          b.nama_wp,
          'DEBET'::varchar                            AS jenis_trx
        FROM t_skab a
        LEFT JOIN m_wajib_pajak b  ON b.id      = a.id_wp
        LEFT JOIN m_kartu mk       ON mk.no_rfid = a.no_rfid
        WHERE concat(mk.kode_kartu, a.no_rfid, a.no_va) ILIKE '%' || p_no_rfid || '%'
        LIMIT 10
      ) x
      UNION ALL
      -- KREDIT: pengajuan topup yang sudah diapprove (penambahan saldo)
      SELECT y.* FROM (
        SELECT
          coalesce(coalesce(a.updated_at, a.created_at), now())::timestamp without time zone AS time_tapping,
          a.no_rfid,
          a.no_va,
          mk.kode_kartu,
          a.nominal,
          b.nomor                                     AS trx_no,
          'BANK JATIM'::varchar                       AS gate_taping,
          c.nama_wp,
          'KREDIT'::varchar                           AS jenis_trx
        FROM t_pengajuan_topup_detail a
        LEFT JOIN t_pengajuan_topup b  ON b.id      = a.id_pengajuan_topup
        LEFT JOIN m_wajib_pajak c      ON c.id      = b.id_wp
        LEFT JOIN m_kartu mk           ON mk.no_rfid = a.no_rfid
        WHERE concat(mk.kode_kartu, a.no_rfid, a.no_va) ILIKE '%' || p_no_rfid || '%'
          AND coalesce(b.is_deleted, false) = false
        LIMIT 10
      ) y
    ) al ORDER BY al.time_tapping ASC
  LOOP
    RETURN NEXT rcd;
  END LOOP;
END
$BODY$
  LANGUAGE plpgsql VOLATILE COST 100 ROWS 1000;
COMMENT ON FUNCTION "public"."fn_list_history_kartu"(varchar)
  IS 'Menampilkan history debet (tapping gate) dan kredit (topup) per kartu RFID';


-- =============================================================================
-- 22. PRIMARY KEY & UNIQUE CONSTRAINT
-- =============================================================================
ALTER TABLE "public"."m_bidang"                ADD CONSTRAINT "m_bidang_pkey"                    PRIMARY KEY ("id");
ALTER TABLE "public"."m_unit_organisasi_kerja" ADD CONSTRAINT "m_unit_organisasi_kerja_pkey"      PRIMARY KEY ("id");
ALTER TABLE "public"."m_golongan"              ADD CONSTRAINT "m_golongan_pkey"                   PRIMARY KEY ("id");
ALTER TABLE "public"."m_jabatan"               ADD CONSTRAINT "m_jabatan_pkey"                    PRIMARY KEY ("id");
ALTER TABLE "public"."m_fungsionalitas"        ADD CONSTRAINT "m_fungsionalitas_pkey"             PRIMARY KEY ("id");
ALTER TABLE "public"."m_pegawai"               ADD CONSTRAINT "m_pegawai_pkey"                    PRIMARY KEY ("id");
ALTER TABLE "public"."auth_role"               ADD CONSTRAINT "c_security_role_pkey"              PRIMARY KEY ("id");
ALTER TABLE "public"."auth_role"               ADD CONSTRAINT "auth_role_name_key"                UNIQUE ("nama");
ALTER TABLE "public"."auth_user"               ADD CONSTRAINT "auth_user_pkey"                    PRIMARY KEY ("id");
ALTER TABLE "public"."auth_user"               ADD CONSTRAINT "auth_user_username_unique"         UNIQUE ("username");
ALTER TABLE "public"."auth_user"               ADD CONSTRAINT "auth_user_email_unique"            UNIQUE ("email");
ALTER TABLE "public"."menu"                    ADD CONSTRAINT "menu_pk"                           PRIMARY KEY ("id");
ALTER TABLE "public"."menu_user"               ADD CONSTRAINT "menu_user_pk"                      PRIMARY KEY ("id");
ALTER TABLE "public"."m_lokasi"                ADD CONSTRAINT "m_lokasi_pk"                       PRIMARY KEY ("id");
ALTER TABLE "public"."m_lokasi"                ADD CONSTRAINT "m_lokasi_un"                       UNIQUE ("kode");
ALTER TABLE "public"."m_kelompok_komoditas"    ADD CONSTRAINT "m_kelompok_komoditas_pk"           PRIMARY KEY ("id");
ALTER TABLE "public"."m_komoditas"             ADD CONSTRAINT "m_komoditas_pk"                    PRIMARY KEY ("id");
ALTER TABLE "public"."m_wajib_pajak"           ADD CONSTRAINT "m_wajib_pajak_pk"                  PRIMARY KEY ("id");
ALTER TABLE "public"."m_kartu"                 ADD CONSTRAINT "m_kartu_pk"                        PRIMARY KEY ("id");
ALTER TABLE "public"."m_kartu"                 ADD CONSTRAINT "m_kartu_un"                        UNIQUE ("no_rfid");
ALTER TABLE "public"."t_skab"                  ADD CONSTRAINT "t_skab_pk"                         PRIMARY KEY ("id");
ALTER TABLE "public"."t_skab"                  ADD CONSTRAINT "t_skab_trx_no_uk"                  UNIQUE ("trx_no");
ALTER TABLE "public"."t_pengajuan_topup"       ADD CONSTRAINT "t_pengajuan_topup_pk"              PRIMARY KEY ("id");
ALTER TABLE "public"."t_pengajuan_topup_detail" ADD CONSTRAINT "t_pengajuan_topup_detail_pk"     PRIMARY KEY ("id");


-- =============================================================================
-- 23. INDEX
-- =============================================================================
-- auth_user
CREATE INDEX "idx_auth_user_id_role"    ON "public"."auth_user" ("id_role");
CREATE INDEX "idx_auth_user_active"     ON "public"."auth_user" ("active", "is_deleted");
CREATE INDEX "idx_auth_user_id_wp"      ON "public"."auth_user" ("id_wp");
CREATE INDEX "idx_auth_user_id_pegawai" ON "public"."auth_user" ("id_pegawai");

-- menu_user
CREATE INDEX "idx_menu_user_id_role"    ON "public"."menu_user" ("id_role");
CREATE INDEX "idx_menu_user_id_menu"    ON "public"."menu_user" ("id_menu");

-- m_komoditas
CREATE INDEX "idx_m_komoditas_kelompok" ON "public"."m_komoditas" ("id_kelompok_komoditas");

-- m_wajib_pajak
CREATE INDEX "idx_m_wajib_pajak_nama"   ON "public"."m_wajib_pajak" ("nama_wp");
CREATE INDEX "idx_m_wajib_pajak_aktif"  ON "public"."m_wajib_pajak" ("is_deleted");

-- m_kartu
CREATE INDEX "idx_m_kartu_no_rfid"      ON "public"."m_kartu" ("no_rfid");
CREATE INDEX "idx_m_kartu_id_wp"        ON "public"."m_kartu" ("id_wp");
CREATE INDEX "idx_m_kartu_id_komoditas" ON "public"."m_kartu" ("id_komoditas");
CREATE INDEX "idx_m_kartu_aktif"        ON "public"."m_kartu" ("aktif", "is_deleted");

-- t_skab
CREATE INDEX "idx_t_skab"               ON "public"."t_skab" ("tanggal", "id_wp");
CREATE INDEX "t_skab_id_wp_idx"         ON "public"."t_skab" ("id_wp");
CREATE INDEX "t_skab_no_rfid_idx"       ON "public"."t_skab" ("no_rfid");
CREATE INDEX "t_skab_tanggal_idx"       ON "public"."t_skab" ("tanggal");

-- t_pengajuan_topup
CREATE INDEX "idx_topup_id_wp"          ON "public"."t_pengajuan_topup" ("id_wp");
CREATE INDEX "idx_topup_status"         ON "public"."t_pengajuan_topup" ("status");
CREATE INDEX "idx_topup_tanggal"        ON "public"."t_pengajuan_topup" ("tanggal");
CREATE INDEX "idx_topup_is_deleted"     ON "public"."t_pengajuan_topup" ("is_deleted");

-- t_pengajuan_topup_detail
CREATE INDEX "idx_topup_detail_pengajuan" ON "public"."t_pengajuan_topup_detail" ("id_pengajuan_topup");
CREATE INDEX "idx_topup_detail_no_rfid"   ON "public"."t_pengajuan_topup_detail" ("no_rfid");
CREATE INDEX "idx_topup_detail_status"    ON "public"."t_pengajuan_topup_detail" ("status_approve_bjtm");


-- =============================================================================
-- 24. FOREIGN KEY
-- =============================================================================
-- m_unit_organisasi_kerja -> m_bidang
ALTER TABLE "public"."m_unit_organisasi_kerja"
  ADD CONSTRAINT "m_unit_organisasi_kerja_id_bidang_fkey"
  FOREIGN KEY ("id_bidang") REFERENCES "public"."m_bidang" ("id")
  ON DELETE NO ACTION ON UPDATE NO ACTION;

-- m_pegawai -> m_unit_organisasi_kerja, m_golongan, m_jabatan
ALTER TABLE "public"."m_pegawai"
  ADD CONSTRAINT "m_pegawai_id_unor_fkey"
  FOREIGN KEY ("id_unor") REFERENCES "public"."m_unit_organisasi_kerja" ("id")
  ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."m_pegawai"
  ADD CONSTRAINT "m_pegawai_id_golongan_fkey"
  FOREIGN KEY ("id_golongan") REFERENCES "public"."m_golongan" ("id")
  ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."m_pegawai"
  ADD CONSTRAINT "m_pegawai_id_jabatan_fkey"
  FOREIGN KEY ("id_jabatan") REFERENCES "public"."m_jabatan" ("id")
  ON DELETE NO ACTION ON UPDATE NO ACTION;

-- auth_user -> auth_role
ALTER TABLE "public"."auth_user"
  ADD CONSTRAINT "auth_user_role_fk"
  FOREIGN KEY ("id_role") REFERENCES "public"."auth_role" ("id")
  ON DELETE NO ACTION ON UPDATE NO ACTION;

-- menu_user -> menu
ALTER TABLE "public"."menu_user"
  ADD CONSTRAINT "menu_user_id_fk"
  FOREIGN KEY ("id_menu") REFERENCES "public"."menu" ("id")
  ON DELETE NO ACTION ON UPDATE NO ACTION;

-- m_komoditas -> m_kelompok_komoditas
ALTER TABLE "public"."m_komoditas"
  ADD CONSTRAINT "m_komoditas_kelompok_fk"
  FOREIGN KEY ("id_kelompok_komoditas") REFERENCES "public"."m_kelompok_komoditas" ("id")
  ON DELETE NO ACTION ON UPDATE NO ACTION;

-- t_pengajuan_topup -> m_wajib_pajak
ALTER TABLE "public"."t_pengajuan_topup"
  ADD CONSTRAINT "t_pengajuan_topup_wp_fk"
  FOREIGN KEY ("id_wp") REFERENCES "public"."m_wajib_pajak" ("id")
  ON DELETE NO ACTION ON UPDATE NO ACTION;

-- t_pengajuan_topup_detail -> t_pengajuan_topup
ALTER TABLE "public"."t_pengajuan_topup_detail"
  ADD CONSTRAINT "t_pengajuan_topup_detail_header_fk"
  FOREIGN KEY ("id_pengajuan_topup") REFERENCES "public"."t_pengajuan_topup" ("id")
  ON DELETE NO ACTION ON UPDATE NO ACTION;

-- =============================================================================
-- SELESAI
-- =============================================================================


-- =============================================================================
-- BAGIAN C — JEMBATAN AUTH_USER (e-PASIR) <-> USERS (Spring Security)
-- =============================================================================
-- Tujuan: menggabungkan 2 sistem auth jadi satu, sesuai keputusan Anda,
-- TANPA mengubah/menghapus apapun yang sudah ada di auth_user maupun users.
--
-- Pendekatan: tambah 1 kolom nullable di auth_user yang menunjuk ke users.id.
-- - Nullable -> baris auth_user lama yang belum punya pasangan di "users"
--   tetap valid, tidak ada baris yang gagal/ter-block.
-- - ON DELETE SET NULL -> kalau suatu akun di "users" dihapus, baris
--   auth_user terkait TIDAK ikut terhapus (data transaksi e-PASIR aman),
--   hanya link-nya yang lepas.
-- - UNIQUE -> satu akun "users" hanya boleh terhubung ke maksimal satu
--   auth_user (1:1), mencegah ambiguitas identitas saat login.
--
-- Migrasi data akun lama ke "users" bisa dilakukan belakangan secara
-- terkontrol (per role/per batch), kolom ini hanya menyediakan tempatnya.

ALTER TABLE "public"."auth_user"
  ADD COLUMN IF NOT EXISTS "id_app_user" UUID;

COMMENT ON COLUMN "public"."auth_user"."id_app_user"
  IS 'Link opsional ke users.id (Spring Security) untuk migrasi/penyatuan akun login. NULL = akun e-PASIR ini belum ditautkan.';

ALTER TABLE "public"."auth_user"
  ADD CONSTRAINT "auth_user_app_user_fk"
  FOREIGN KEY ("id_app_user") REFERENCES "public"."users" ("id")
  ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE "public"."auth_user"
  ADD CONSTRAINT "auth_user_app_user_unique" UNIQUE ("id_app_user");

CREATE INDEX IF NOT EXISTS "idx_auth_user_app_user" ON "public"."auth_user" ("id_app_user");

-- =============================================================================
-- SELESAI — commit seluruh perubahan sebagai satu transaksi
-- =============================================================================
COMMIT;
