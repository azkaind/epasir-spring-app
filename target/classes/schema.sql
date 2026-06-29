-- -- ============================================================
-- -- SCHEMA LENGKAP v2 — Spring Security App
-- -- Menggabungkan v1 (auth) + v2 (role management & permissions)
-- -- Jalankan sebagai: psql -U postgres -d security_db -f schema.sql
-- -- ============================================================

-- -- ── 1. TABEL APP_ROLES (harus sebelum users) ──────────────────────────────
-- CREATE TABLE IF NOT EXISTS app_roles (
--     id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
--     name           VARCHAR(50)  NOT NULL UNIQUE,
--     description    VARCHAR(200),
--     color          VARCHAR(10)  NOT NULL DEFAULT '#1A237E',
--     is_system_role BOOLEAN      NOT NULL DEFAULT FALSE,
--     created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
--     updated_at     TIMESTAMP
-- );

-- CREATE INDEX IF NOT EXISTS idx_role_name ON app_roles(name);

-- -- ── 2. TABEL ROLE_PERMISSIONS ──────────────────────────────────────────────
-- CREATE TABLE IF NOT EXISTS role_permissions (
--     role_id    UUID        NOT NULL REFERENCES app_roles(id) ON DELETE CASCADE,
--     permission VARCHAR(60) NOT NULL,
--     PRIMARY KEY (role_id, permission)
-- );

-- -- ── 3. TABEL USERS ────────────────────────────────────────────────────────
-- CREATE TABLE IF NOT EXISTS users (
--     id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
--     username              VARCHAR(50)  NOT NULL UNIQUE,
--     email                 VARCHAR(100) NOT NULL UNIQUE,
--     password              VARCHAR(255) NOT NULL,
--     full_name             VARCHAR(100),
--     phone_number          VARCHAR(20),
--     role_id               UUID         REFERENCES app_roles(id),
--     is_enabled            BOOLEAN      NOT NULL DEFAULT TRUE,
--     is_account_non_locked BOOLEAN      NOT NULL DEFAULT TRUE,
--     failed_login_attempts INTEGER      NOT NULL DEFAULT 0,
--     lock_time             TIMESTAMP,
--     last_login            TIMESTAMP,
--     created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
--     updated_at            TIMESTAMP
-- );

-- CREATE INDEX IF NOT EXISTS idx_users_email    ON users(email);
-- CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
-- CREATE INDEX IF NOT EXISTS idx_users_role_id  ON users(role_id);

-- -- ── 4. TABEL REFRESH_TOKENS ───────────────────────────────────────────────
-- CREATE TABLE IF NOT EXISTS refresh_tokens (
--     id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
--     token       VARCHAR(512) NOT NULL UNIQUE,
--     user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
--     expires_at  TIMESTAMP    NOT NULL,
--     is_revoked  BOOLEAN      NOT NULL DEFAULT FALSE,
--     device_info VARCHAR(255),
--     created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
-- );

-- CREATE INDEX IF NOT EXISTS idx_refresh_token_value ON refresh_tokens(token);
-- CREATE INDEX IF NOT EXISTS idx_refresh_token_user  ON refresh_tokens(user_id);

-- -- ── 5. TABEL AUDIT_LOGS ───────────────────────────────────────────────────
-- CREATE TABLE IF NOT EXISTS audit_logs (
--     id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
--     username      VARCHAR(100),
--     action        VARCHAR(100) NOT NULL,
--     ip_address    VARCHAR(50),
--     user_agent    VARCHAR(255),
--     resource_path VARCHAR(255),
--     status        VARCHAR(20),
--     details       VARCHAR(500),
--     created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
-- );

-- CREATE INDEX IF NOT EXISTS idx_audit_username   ON audit_logs(username);
-- CREATE INDEX IF NOT EXISTS idx_audit_action     ON audit_logs(action);
-- CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs(created_at DESC);

-- -- ── 6. SEED ROLES DEFAULT ─────────────────────────────────────────────────

-- INSERT INTO app_roles (id, name, description, color, is_system_role)
-- VALUES
--   ('11111111-1111-1111-1111-111111111111',
--    'ROLE_ADMIN', 'Administrator dengan akses penuh ke semua fitur sistem', '#B71C1C', TRUE),
--   ('22222222-2222-2222-2222-222222222222',
--    'ROLE_MODERATOR', 'Moderator dengan akses baca dan kunci pengguna', '#1565C0', TRUE),
--   ('33333333-3333-3333-3333-333333333333',
--    'ROLE_USER', 'Pengguna standar dengan akses terbatas', '#2E7D32', TRUE)
-- ON CONFLICT (name) DO NOTHING;

-- -- Permissions untuk ROLE_ADMIN (semua)
-- INSERT INTO role_permissions (role_id, permission)
-- SELECT '11111111-1111-1111-1111-111111111111', p FROM unnest(ARRAY[
--   'USER_VIEW','USER_CREATE','USER_EDIT','USER_DELETE','USER_LOCK','USER_ASSIGN_ROLE',
--   'ROLE_VIEW','ROLE_CREATE','ROLE_EDIT','ROLE_DELETE','ROLE_ASSIGN_PERMISSION',
--   'AUDIT_VIEW','AUDIT_EXPORT','DASHBOARD_VIEW','DASHBOARD_STATS'
-- ]) AS p ON CONFLICT DO NOTHING;

-- -- Permissions untuk ROLE_MODERATOR
-- INSERT INTO role_permissions (role_id, permission)
-- SELECT '22222222-2222-2222-2222-222222222222', p FROM unnest(ARRAY[
--   'USER_VIEW','USER_LOCK','ROLE_VIEW','AUDIT_VIEW','DASHBOARD_VIEW','DASHBOARD_STATS'
-- ]) AS p ON CONFLICT DO NOTHING;

-- -- Permissions untuk ROLE_USER
-- INSERT INTO role_permissions (role_id, permission)
-- SELECT '33333333-3333-3333-3333-333333333333', p FROM unnest(ARRAY[
--   'DASHBOARD_VIEW'
-- ]) AS p ON CONFLICT DO NOTHING;

-- -- ── 7. SEED USER ADMIN DEFAULT ────────────────────────────────────────────
-- -- Password: Admin@1234 (BCrypt strength 12)
-- INSERT INTO users (username, email, password, full_name, role_id, created_at)
-- VALUES (
--   'admin', 'admin@example.com',
--   '$2a$12$LcVKhx2R7eDe5V1X0h9B/.gX.JaEqVWLh1M7UFLbTkw/P4dOBdxXi',
--   'System Administrator',
--   '11111111-1111-1111-1111-111111111111',
--   NOW()
-- ) ON CONFLICT (username) DO NOTHING;

-- -- ── 8. VIEWS ─────────────────────────────────────────────────────────────

-- CREATE OR REPLACE VIEW v_role_user_counts AS
-- SELECT r.id, r.name, r.description, r.color, r.is_system_role,
--        COUNT(u.id) AS user_count
-- FROM app_roles r
-- LEFT JOIN users u ON u.role_id = r.id
-- GROUP BY r.id, r.name, r.description, r.color, r.is_system_role;

-- CREATE OR REPLACE VIEW v_active_sessions AS
-- SELECT u.username, u.email, COUNT(rt.id) AS active_token_count,
--        MAX(rt.created_at) AS last_token_created
-- FROM users u
-- LEFT JOIN refresh_tokens rt ON rt.user_id = u.id
--     AND rt.is_revoked = FALSE AND rt.expires_at > NOW()
-- GROUP BY u.id, u.username, u.email;

-- CREATE OR REPLACE VIEW v_security_events AS
-- SELECT action, status, COUNT(*) AS event_count, DATE(created_at) AS event_date
-- FROM audit_logs
-- WHERE created_at >= NOW() - INTERVAL '30 days'
-- GROUP BY action, status, DATE(created_at)
-- ORDER BY event_date DESC, event_count DESC;
