# 🔐 Spring Security — Sistem Keamanan Lengkap v2

Implementasi sistem keamanan enterprise dengan **Spring Boot 3** + **PostgreSQL**.
Mencakup autentikasi JWT, RBAC dinamis, permission granular, dan manajemen pengguna lengkap.

---

## 📁 Struktur Proyek

```
src/main/java/com/example/security/
├── config/
│   ├── SecurityConfig.java              ← CORS, stateless, permission granular
│   ├── CustomAuthEntryPoint.java        ← Handler 401
│   └── CustomAccessDeniedHandler.java  ← Handler 403
├── controller/
│   ├── AuthController.java              ← Register, Login, Refresh, Logout
│   ├── UserController.java             ← Profil, Moderator endpoint
│   ├── UserManagementController.java   ← CRUD Pengguna (BARU)
│   └── RoleController.java             ← CRUD Role + Permission (BARU)
├── dto/
│   ├── request/  LoginRequest, RegisterRequest, CreateUserRequest,
│   │             UpdateUserRequest, CreateRoleRequest, UpdateRoleRequest
│   └── response/ ApiResponse, AuthResponse, UserDetailResponse,
│                 RoleResponse, PermissionResponse, UserStatsResponse
├── entity/
│   ├── AppRole.java      ← Role dinamis (BARU, menggantikan enum Role)
│   ├── Permission.java   ← 16 permission granular (BARU)
│   ├── User.java         ← ManyToOne ke AppRole, getAuthorities() granular
│   ├── AuditLog.java
│   └── RefreshToken.java
├── exception/  BadRequestException, UserNotFoundException, dll
├── filter/     JwtAuthenticationFilter
├── repository/ UserRepository, AppRoleRepository, RefreshTokenRepository, AuditLogRepository
├── service/
│   ├── AuthService.java, AuditService.java
│   └── impl/  AuthServiceImpl, UserDetailsServiceImpl,
│              RoleServiceImpl (BARU), UserManagementServiceImpl (BARU)
└── util/       JwtUtil
```

---

## 🛡️ Fitur Keamanan

| Fitur | Detail |
|---|---|
| **JWT + Refresh Token Rotation** | Access 15 menit, Refresh 7 hari, revoke saat logout |
| **BCrypt Password** | Strength 12 |
| **Brute Force Protection** | Kunci 15 menit setelah 5x gagal |
| **RBAC Dinamis** | Role disimpan di DB, bisa CRUD tanpa redeploy |
| **Permission Granular** | 16 permission (user:view, role:delete, audit:view, dll) |
| **Audit Log** | Semua aktivitas tercatat asinkron |
| **Security Headers** | HSTS, CSP, X-Frame-Options |

---

## 📡 API Endpoints

### Auth (Publik)
```
POST /api/auth/register    POST /api/auth/login
POST /api/auth/refresh     POST /api/auth/logout
```

### Pengguna (permission: user:*)
```
GET    /api/users                   → user:view
GET    /api/users/stats             → user:view
GET    /api/users/{id}              → user:view
POST   /api/users                   → user:create
PATCH  /api/users/{id}              → user:edit
DELETE /api/users/{id}              → user:delete
PUT    /api/users/{id}/role         → user:assign_role
PATCH  /api/users/{id}/toggle-lock  → user:lock
PATCH  /api/users/{id}/toggle-enable→ user:lock
```

### Role (permission: role:*)
```
GET    /api/roles                          → role:view
GET    /api/roles/{id}                     → role:view
GET    /api/roles/permissions/all          → role:view
POST   /api/roles                          → role:create
PATCH  /api/roles/{id}                     → role:edit
DELETE /api/roles/{id}                     → role:delete
PUT    /api/roles/{id}/permissions         → role:assign_permission
POST   /api/roles/{id}/permissions/{perm}  → role:assign_permission
DELETE /api/roles/{id}/permissions/{perm}  → role:assign_permission
```

---

## 🚀 Setup

```bash
# 1. Buat database
psql -U postgres -c "CREATE DATABASE security_db;"

# 2. Jalankan schema (otomatis seed 3 role + 1 admin)
psql -U postgres -d security_db -f src/main/resources/schema.sql

# 3. Sesuaikan application.properties (DB password, JWT secret)

# 4. Jalankan
mvn spring-boot:run
```

**Login Admin:** username `admin` / password `Admin@1234`
