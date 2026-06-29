package com.example.security.entity;

/**
 * Daftar permission granular dalam sistem.
 *
 * Dikelompokkan berdasarkan kategori resource:
 *   USER_*       → manajemen pengguna
 *   ROLE_*       → manajemen role
 *   AUDIT_*      → akses log audit
 *   DASHBOARD_*  → akses dashboard & statistik
 *
 * Permission ditetapkan ke AppRole, dan AppRole ditetapkan ke User.
 * Spring Security membaca authority dari kombinasi role + permissions.
 */
public enum Permission {

    // ── USER MANAGEMENT ────────────────────────────────────────────────────
    USER_VIEW       ("user:view",    "Melihat daftar dan detail pengguna",    "Pengguna"),
    USER_CREATE     ("user:create",  "Membuat pengguna baru",                 "Pengguna"),
    USER_EDIT       ("user:edit",    "Mengubah data pengguna",                "Pengguna"),
    USER_DELETE     ("user:delete",  "Menghapus pengguna",                    "Pengguna"),
    USER_LOCK       ("user:lock",    "Mengunci/membuka kunci akun pengguna",  "Pengguna"),
    USER_ASSIGN_ROLE("user:assign_role", "Menetapkan role ke pengguna",       "Pengguna"),

    // ── ROLE MANAGEMENT ────────────────────────────────────────────────────
    ROLE_VIEW       ("role:view",    "Melihat daftar dan detail role",        "Role"),
    ROLE_CREATE     ("role:create",  "Membuat role baru",                     "Role"),
    ROLE_EDIT       ("role:edit",    "Mengubah nama/deskripsi role",          "Role"),
    ROLE_DELETE     ("role:delete",  "Menghapus role",                        "Role"),
    ROLE_ASSIGN_PERMISSION("role:assign_permission",
                           "Menetapkan permission ke role",                   "Role"),

    // ── AUDIT LOG ──────────────────────────────────────────────────────────
    AUDIT_VIEW      ("audit:view",   "Melihat audit log aktivitas",           "Audit"),
    AUDIT_EXPORT    ("audit:export", "Mengekspor audit log ke CSV",           "Audit"),

    // ── DASHBOARD ──────────────────────────────────────────────────────────
    DASHBOARD_VIEW  ("dashboard:view",  "Mengakses dashboard utama",          "Dashboard"),
    DASHBOARD_STATS ("dashboard:stats", "Melihat statistik sistem",           "Dashboard");

    // ──────────────────────────────────────────────────────────────────────

    private final String authority;   // String untuk Spring Security
    private final String description; // Keterangan tampil di UI
    private final String category;    // Grup untuk tampilan UI

    Permission(String authority, String description, String category) {
        this.authority   = authority;
        this.description = description;
        this.category    = category;
    }

    public String getAuthority()   { return authority; }
    public String getDescription() { return description; }
    public String getCategory()    { return category; }

    /** Cari Permission berdasarkan authority string. */
    public static Permission fromAuthority(String authority) {
        for (Permission p : values()) {
            if (p.authority.equals(authority)) return p;
        }
        throw new IllegalArgumentException("Unknown permission: " + authority);
    }
}
